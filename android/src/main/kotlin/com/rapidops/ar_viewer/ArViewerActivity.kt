package com.rapidops.ar_viewer

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.View
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.material.setBaseColorMap
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ArViewerActivity : ComponentActivity() {

    private lateinit var modelUrl: String
    private var savedModelInstance: ModelInstance? = null
    private var materialList = mutableListOf<String>()
    private lateinit var colorList: List<Color>

    var colorMap: MutableList<MaterialInstance> = mutableListOf()  // store material instances and change color
    var selectedColorIndices = mutableStateListOf<Int>()  // store selected color indices
    var imgUri: Uri? = null     // store image uri if image is selected
    var selectedMaterialIndex = 0 // store selected material index
    var engineCopy: Engine? = null

    private lateinit var engine: Engine
    private lateinit var modelLoader: ModelLoader
    private lateinit var materialLoader: MaterialLoader
    private lateinit var cameraNode: ARCameraNode
    private lateinit var childNodes: MutableList<Node>
    private lateinit var anchorNodeMan: Anchor
    private lateinit var view: View
    private lateinit var collisionSystem: CollisionSystem

    private lateinit var frame: Frame
    private var trackingFailureReason by mutableStateOf<TrackingFailureReason?>(null)
    private var errorMessage by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)
    private var planesDetected by mutableStateOf(false)
    private var modelPlaced by mutableStateOf(false)
    private var showScanAnimation by mutableStateOf(true)
    private var showTapAnimation by mutableStateOf(false)
    private var showMoveAnimation by mutableStateOf(false)
    private var tapSession by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        try {
            modelUrl = intent.getStringExtra("MODEL_URL") ?: ""
            val colors = intent.getStringArrayListExtra("MODEL_COLORS")
            colorList = colors?.mapNotNull { parseColor(it) } ?: emptyList()

            setContent {

                engine = rememberEngine()
                modelLoader = rememberModelLoader(engine)
                materialLoader = rememberMaterialLoader(engine)
                cameraNode = rememberARCameraNode(engine)
                childNodes = rememberNodes()
                view = rememberView(engine)
                collisionSystem = rememberCollisionSystem(view)
                rememberScene(engine)
                engineCopy = engine



                ArViewerScreen()
            }
        } catch (e: Exception) {
            Log.e("ArViewerActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing AR viewer: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun parseColor(colorString: String): Color? {
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: IllegalArgumentException) {
            Log.w("ArViewerActivity", "Invalid color: $colorString")
            null
        }
    }

    @Composable
    fun ArViewerScreen() {


        Box(modifier = Modifier.fillMaxSize()) {
            ARScene(modifier = Modifier.fillMaxSize(), childNodes = childNodes, engine = engine, view = view, onViewCreated = {


//                anchorNodeMan = this.session?.createAnchor(frame?.camera?.pose?.compose(Pose.makeTranslation(0f, 0f, -1f)))!!
                this.planeRenderer.planeRendererMode = PlaneRenderer.PlaneRendererMode.RENDER_ALL
            }, modelLoader = modelLoader, collisionSystem = collisionSystem, sessionConfiguration = { session, config ->
                config.apply {
                    depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
                    instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                }
            }, cameraNode = cameraNode, onTrackingFailureChanged = { trackingFailureReason = it }, onSessionUpdated = { _, updatedFrame ->
                frame = updatedFrame
                if (updatedFrame.getUpdatedPlanes().isNotEmpty()) {
                    planesDetected = true
                    showScanAnimation = false
                    if (!tapSession) {
                        showTapAnimation = true
                    }
                }
            }, onGestureListener = rememberOnGestureListener(onSingleTapConfirmed = { motionEvent, _ ->
                if (!isLoading && planesDetected && !modelPlaced) {
                    showTapAnimation = false
                    tapSession = true
                    handleTap(motionEvent, frame, engine, modelLoader, materialLoader, childNodes) {
                        isLoading = it
                        if (!it) {
                            modelPlaced = true
                            showMoveAnimation = true
                        }
                    }
                }
            }))



            Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                InfoText(trackingFailureReason = trackingFailureReason, errorMessage = errorMessage, isLoading = isLoading, planesDetected = planesDetected, modelPlaced = modelPlaced)

                Spacer(modifier = Modifier.weight(1f))

                Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        showScanAnimation -> ScanAnimation()
                        showTapAnimation && !isLoading -> TapAnimation()
                        showMoveAnimation -> MoveAnimation(showMoveAnimation = showMoveAnimation, onAnimationComplete = { showMoveAnimation = false })
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                BottomControls(colorList)
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center), color = Color.White)
            }
        }
    }

    @Composable
    fun ScanAnimation() {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation_scan))
        val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever)
        LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.height(300.dp),
        )
    }

    @Composable
    fun TapAnimation() {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation_tap))
        val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever)
        LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.height(300.dp),
        )
    }

    @Composable
    fun MoveAnimation(showMoveAnimation: Boolean, onAnimationComplete: () -> Unit) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation_move))
        val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)

        if (showMoveAnimation) {
            LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.height(300.dp),
            )

            if (progress == 1.0f) {
                onAnimationComplete()
            }
        }
    }

    @Composable
    fun InfoText(
            trackingFailureReason: TrackingFailureReason?,
            errorMessage: String?,
            isLoading: Boolean,
            planesDetected: Boolean,
            modelPlaced: Boolean,
    ) {
        val context = LocalContext.current
        val text = when {
            errorMessage != null -> errorMessage
            isLoading -> stringResource(R.string.loading_model)
            trackingFailureReason != null -> trackingFailureReason.getDescription(context)
            !planesDetected -> stringResource(R.string.move_phone_to_detect_surfaces)
            !modelPlaced -> stringResource(R.string.tap_anywhere_to_add_model)
            else -> stringResource(R.string.model_placed)
        }

        Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)) {
            Text(modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 32.dp), textAlign = TextAlign.Center, fontSize = 18.sp, color = Color.White, text = text)
        }
    }

    @Composable
    fun BottomControls(colors: List<Color>) {
        var isListVisible by remember { mutableStateOf(false) }
        var selectedMaterialIndex by remember { mutableIntStateOf(0) }

        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
        ) {
            if (isListVisible) {
                VerticalList(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp), onItemClick = {
                    selectedMaterialIndex = it
                    isListVisible = false
                })
            }

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
            ) {
                Button(onClick = {

                    loadModel()


                }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Reset", tint = Color.Black)
                }

                LazyRow(modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(colors) { color ->
                        ColorButton(color = color, isSelected = selectedColorIndices.getOrNull(selectedMaterialIndex) == colors.indexOf(color), onColorSelected = { selectedColor ->
                            setColor(selectedColor, selectedMaterialIndex)
                            if (selectedMaterialIndex >= selectedColorIndices.size) {
                                selectedColorIndices.add(colors.indexOf(selectedColor))
                            } else {
                                selectedColorIndices[selectedMaterialIndex] = colors.indexOf(selectedColor)
                            }
                        })
                    }
                }

                Column {
                    Button(
                            onClick = { isListVisible = !isListVisible },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                    ) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = "More Options", tint = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = { imagePicker() }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
                        Icon(imageVector = Icons.Rounded.Face, contentDescription = "Image", tint = Color.Black)
                    }
                }
            }
        }
    }

    @Composable
    fun ColorButton(color: Color, isSelected: Boolean, onColorSelected: (Color) -> Unit) {
        Box(modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
                .border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) Color.White else Color.Transparent, shape = CircleShape)) {
            Button(
                    onClick = { onColorSelected(color) },
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = color),
            ) {}
        }
    }

    @Composable
    fun VerticalList(modifier: Modifier = Modifier, onItemClick: (Int) -> Unit) {
        LazyColumn(modifier = modifier
                .background(Color.White.copy(alpha = 0.8f))
                .padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(materialList) { index, item ->
                Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(index) }
                        .padding(vertical = 8.dp, horizontal = 16.dp)) {
                    Text(text = item, color = Color.Black)
                }
            }
        }
    }

    private fun handleTap(
            motionEvent: MotionEvent,
            frame: Frame?,
            engine: Engine,
            modelLoader: ModelLoader,
            materialLoader: MaterialLoader,
            childNodes: MutableList<*>,
            setLoading: (Boolean) -> Unit,
    ) {
        val currentFrame = frame // Capture frame into a local variable

        currentFrame?.hitTest(motionEvent.x, motionEvent.y)?.firstOrNull { it.isValid(depthPoint = false, point = false) }?.createAnchorOrNull()?.let { anchor ->
            setLoading(true)
            lifecycleScope.launch {
                try {
                    (childNodes as MutableList<AnchorNode>).clear()


                    val anchorNode = createAnchorNode(engine, modelLoader, materialLoader, anchor)


                    childNodes.add(anchorNode)
                } catch (e: Exception) {
                    // Handle exception
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private suspend fun createAnchorNode(
            engine: Engine,
            modelLoader: ModelLoader,
            materialLoader: MaterialLoader,
            anchor: Anchor,
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = savedModelInstance?.let {
            ModelNode(modelInstance = it, scaleToUnits = 0.5f)
        } ?: modelLoader.loadModelInstance(modelUrl)?.also {
            savedModelInstance = it
        }?.let { modelInstance ->
            modelInstance.materialInstances.forEachIndexed { index, materialInstance ->
                materialList.add(materialInstance.name)
                if (index < colorMap.size) {
                    colorMap[index] = materialInstance
                } else {
                    colorMap.add(materialInstance)
                }
            }
            ModelNode(modelInstance = modelInstance, scaleToUnits = 0.5f)
        } ?: throw IllegalStateException("Failed to load model")

        modelNode.apply {
            isEditable = true
            editableScaleRange = 0.2f..0.75f
            position = Position(0f, 0f, 0f)
        }

        anchorNode.addChildNode(modelNode)
        return anchorNode
    }

    private fun setColor(color: Color, materialIndex: Int) {
        val r = color.red
        val g = color.green
        val b = color.blue


        if (materialIndex in colorMap.indices) {
            val texture = createTextureFromUriAndColor()
            colorMap[materialIndex] = colorMap[materialIndex].apply {
                // Set the base color factor to the desired color
                setParameter("baseColorFactor", r, g, b, 1.0f)
                if (texture != null) {
                    setBaseColorMap(texture)
                }
            }
        }

    }

    private fun loadModel() {


        val currentFrame = frame // Capture frame into a local variable


        val centerX = currentFrame.camera.textureIntrinsics.imageDimensions[0] / 2
        val centerY = currentFrame.camera.textureIntrinsics.imageDimensions[1] / 2
        val anchor = currentFrame.hitTest(centerX.toFloat(), centerY.toFloat()).firstOrNull { it.trackable is Plane && it.trackable.trackingState == TrackingState.TRACKING }?.createAnchorOrNull()



        if (anchor != null) {

            lifecycleScope.launch {
                try {
                    // Clear existing anchor nodes if necessary

                    for (node in childNodes) {
                        node.parent = null
                    }
                    childNodes.clear()

                    savedModelInstance = null

                    // Create the anchor node
                    val anchorNode = createAnchorNode(engine, modelLoader, materialLoader, anchor)

                    // Add the anchor node to the list of child nodes
                    childNodes.add(anchorNode)

                    // Optionally handle success or perform additional actions
                } catch (e: Exception) {
                    // Handle any exceptions that occur during anchor node creation
                    Log.e("ArViewerActivity", "Error creating anchor node: ${e.message}", e)
                    Toast.makeText(this@ArViewerActivity, "Failed to create anchor node", Toast.LENGTH_SHORT).show()
                    // Optionally show an error message or perform error handling
                }
            }
        } else {
            // Handle case where anchor is null (e.g., no valid hit test result)
            Log.w("ArViewerActivity", "No valid anchor found at touch location.")
            Toast.makeText(this@ArViewerActivity, "anchor node is null", Toast.LENGTH_SHORT).show()

            // Optionally show a message to the user or perform error handling
        }

    }


    private fun imagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        changeImage.launch(intent)
    }

    private val changeImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val data = it.data
            imgUri = data?.data
            imgUri?.let { uri ->
                lifecycleScope.launch {
                    try {
                        val texture = withContext(Dispatchers.Default) {
                            createTextureFromUriAndColor()
                        }
                        withContext(Dispatchers.Main) {
                            if (texture != null) {
                                applyTextureToMaterial(texture)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ArViewerActivity", "Error creating texture", e)
                        Toast.makeText(this@ArViewerActivity, "Failed to create texture", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun createTextureFromUriAndColor(): Texture? {
        val buffer: ByteBuffer
        val texture: Texture

        return try {
            if (imgUri != null) {
                Log.d("ImageURI", "IMAGE URI IS $imgUri")
                val inputStream = contentResolver.openInputStream(imgUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val width = bitmap.width
                val height = bitmap.height
                buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
                bitmap.copyPixelsToBuffer(buffer)
                buffer.flip()

                texture = Texture.Builder().width(width).height(height).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).build(engineCopy!!)

                val pixelBufferDescriptor = Texture.PixelBufferDescriptor(buffer, Texture.Format.RGBA, Texture.Type.UBYTE)
                texture.setImage(engineCopy!!, 0, pixelBufferDescriptor)

                texture
            } else {
                // Handle case where imgUri is null (if needed)
                buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
                buffer.put(0, 255.toByte()) // Red
                buffer.put(1, 255.toByte()) // Green
                buffer.put(2, 255.toByte()) // Blue
                buffer.put(3, 255.toByte()) // Alpha

                texture = Texture.Builder().width(1).height(1).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).build(engineCopy!!)

                val pixelBufferDescriptor = Texture.PixelBufferDescriptor(buffer, Texture.Format.RGBA, Texture.Type.UBYTE)
                texture.setImage(engineCopy!!, 0, pixelBufferDescriptor)
                texture
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create texture", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun applyTextureToMaterial(texture: Texture) {
        if (selectedMaterialIndex in colorMap.indices) {
            colorMap[selectedMaterialIndex].setBaseColorMap(texture)
        } else {
            Log.e("LogDB", "Invalid selectedMaterialIndex: $selectedMaterialIndex")
        }
    }
}

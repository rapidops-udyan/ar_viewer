package com.rapidops.ar_viewer

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import kotlinx.coroutines.launch

class ArViewerActivity : ComponentActivity() {

    private lateinit var modelUrl: String
    private var savedModelInstance: ModelInstance? = null
    private var materialList = mutableListOf<String>()
    private lateinit var colorList: List<Color>


    //        store glb file material list name
    var colorMap: MutableList<MaterialInstance> = mutableListOf()
    var defaultMaterial: MutableList<MaterialInstance> = mutableListOf()
    var selectedColorIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        try {
            modelUrl = intent.getStringExtra("MODEL_URL") ?: ""
            val colors = intent.getStringArrayListExtra("MODEL_COLORS")
            colorList = colors?.mapNotNull { parseColor(it) } ?: emptyList()

            setContent {
                ArViewerScreen()
            }
        } catch (e: Exception) {
            Log.e("ArViewerActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing AR viewer: ${e.message}", Toast.LENGTH_LONG)
                    .show()
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
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val materialLoader = rememberMaterialLoader(engine)
        val cameraNode = rememberARCameraNode(engine)
        val childNodes = rememberNodes()
        val view = rememberView(engine)
        val collisionSystem = rememberCollisionSystem(view)
        rememberScene(engine)

        var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
        var frame by remember { mutableStateOf<Frame?>(null) }
        val errorMessage by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {

            ARScene(
                    modifier = Modifier.fillMaxSize(),
                    childNodes = childNodes,
                    engine = engine,
                    view = view,
                    modelLoader = modelLoader,
                    collisionSystem = collisionSystem,
                    sessionConfiguration = { session, config ->
                        config.apply {
                            depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                Config.DepthMode.AUTOMATIC
                            } else {
                                Config.DepthMode.DISABLED
                            }
                            instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        }
                    },
                    cameraNode = cameraNode,
                    onViewCreated = {
                        this.planeRenderer.planeRendererMode =
                                PlaneRenderer.PlaneRendererMode.RENDER_ALL
                    }
                            onTrackingFailureChanged = { trackingFailureReason = it },
                    onSessionUpdated = { _, updatedFrame -> frame = updatedFrame },
                    onGestureListener = rememberOnGestureListener(onSingleTapConfirmed = { motionEvent, _ ->
                        if (!isLoading) {
                            handleTap(
                                    motionEvent, frame, engine, modelLoader, materialLoader, childNodes
                            ) {
                                isLoading = it
                            }
                        }
                    })
            )

            Column(
                    modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
            ) {
                InfoText(
                        trackingFailureReason = trackingFailureReason,
                        childNodesEmpty = childNodes.isEmpty(),
                        errorMessage = errorMessage,
                        isLoading = isLoading
                )

                Spacer(modifier = Modifier.weight(1f))

                BottomControls(colorList) {}
            }

            if (isLoading) {
                CircularProgressIndicator(
                        modifier = Modifier
                                .size(50.dp)
                                .align(Alignment.Center), color = Color.White
                )
            }
        }
    }

    @Composable
    fun BottomControls(colors: List<Color>, onColorSelected: (Color) -> Unit) {
        var isListVisible by remember { mutableStateOf(false) }

        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
        ) {
            if (isListVisible) {
                VerticalList(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        onItemClick = {
                            selectedColorIndex = it
                            isListVisible = false
                        }
                )
            }
//show Bottom Row
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
            ) {
//                Reset Button for set Material to default
                Button(
                        onClick = { /* Reset functionality */
                            resetColors()
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                ) {
                    Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset",
                            tint = Color.Black
                    )
                }
//                Show Color List for change Material Color
                LazyRow(
                        modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(colors) { color ->
                        ColorButton(color = color, onColorSelected = onColorSelected)
                    }
                }

                Column {
                    Button(
                            onClick = { isListVisible = !isListVisible },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "More Options",
                                tint = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                            onClick = {
                                /* Image functionality */
                                imagePicker()
                            },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Icon(
                                imageVector = Icons.Rounded.Face,
                                contentDescription = "Image",
                                tint = Color.Black
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ColorButton(color: Color, onColorSelected: (Color) -> Unit) {
        Button(
                onClick = {
                    onColorSelected(color)
                    setColor(color)
                },
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = color)
        ) {}
    }

    @Composable
    fun VerticalList(modifier: Modifier = Modifier, onItemClick: (Int) -> Unit) {
        val listItems = materialList

        LazyColumn(
                modifier = modifier
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(listItems) { index, item ->
                Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onItemClick(index)
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp)) {
                    Text(
                            text = item, color = Color.Black
                    )
                }
            }
        }
    }

    @Composable
    fun InfoText(
            trackingFailureReason: TrackingFailureReason?,
            childNodesEmpty: Boolean,
            errorMessage: String?,
            isLoading: Boolean,
    ) {
        val context = LocalContext.current
        val text = when {
            errorMessage != null -> errorMessage
            isLoading -> stringResource(R.string.loading_model)
            trackingFailureReason != null -> trackingFailureReason.getDescription(context)
            childNodesEmpty -> stringResource(R.string.tap_anywhere_to_add_model)
            else -> stringResource(R.string.model_placed)
        }

        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
        ) {
            Text(
                    modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = Color.White,
                    text = text
            )
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
        frame?.hitTest(motionEvent.x, motionEvent.y)
                ?.firstOrNull { it.isValid(depthPoint = false, point = false) }?.createAnchorOrNull()
                ?.let { anchor ->
                    setLoading(true)
                    lifecycleScope.launch {
                        try {
                            (childNodes as MutableList<AnchorNode>).clear()
                            val anchorNode =
                                    createAnchorNode(engine, modelLoader, materialLoader, anchor)
                            childNodes.add(anchorNode)
                        } catch (e: Exception) {
                            // Handle exception
                        } finally {
                            setLoading(false)
                        }
                    }
                }
    }

    //    Create Anchor Node and Load Glb File
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
//                add to materialList
                materialList.add(materialInstance.name)
                if (index < colorMap.size) {
                    colorMap[index] = materialInstance
                    defaultMaterial[index] = materialInstance
                } else {
                    colorMap.add(materialInstance)
                    defaultMaterial.add(materialInstance)
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

    //    set Color to selected material
    private fun setColor(color: Color) {
        // Extract the red, green, and blue components
        val r = color.red
        val g = color.green
        val b = color.blue

        // Set the color using the RGB values
        colorMap[selectedColorIndex] = colorMap[selectedColorIndex].apply {
            setParameter("baseColorFactor", r, g, b, 1.0f)
        }
    }


    //    reset all colors to default
    private fun resetColors() {
        colorMap.forEachIndexed { index, _ ->
            val defaultInstance = defaultMaterial.getOrNull(index)
            if (defaultInstance != null) {
                colorMap[index] = defaultInstance
            }
        }
    }

    private fun imagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivity(intent)
        Log.d("ImagePicker", "Image Picker Called-> ${intent}")
    }
}
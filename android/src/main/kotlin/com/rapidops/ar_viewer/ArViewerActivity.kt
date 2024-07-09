package com.rapidops.ar_viewer

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
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
import io.github.sceneview.rememberView
import kotlinx.coroutines.launch

class ArViewerActivity : ComponentActivity() {
    private lateinit var modelUrl: String
    private var savedModelInstance: ModelInstance? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelUrl = intent.getStringExtra("MODEL_URL") ?: ""

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)
                val materialLoader = rememberMaterialLoader(engine)
                val cameraNode = rememberARCameraNode(engine)
                val childNodes = rememberNodes()
                val view = rememberView(engine)
                val collisionSystem = rememberCollisionSystem(view)

                var planeRenderer by remember { mutableStateOf(true) }
                var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
                var frame by remember { mutableStateOf<Frame?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(false) }

                ARScene(
                    modifier = Modifier.fillMaxSize(),
                    childNodes = childNodes,
                    engine = engine,
                    view = view,
                    modelLoader = modelLoader,
                    collisionSystem = collisionSystem,
                    sessionConfiguration = { session, config ->
                        config.apply {
                            depthMode =
                                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                    Config.DepthMode.AUTOMATIC
                                } else {
                                    Config.DepthMode.DISABLED
                                }
                            instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        }
                    },
                    cameraNode = cameraNode,
                    planeRenderer = planeRenderer,
                    onTrackingFailureChanged = { trackingFailureReason = it },
                    onSessionUpdated = { _, updatedFrame -> frame = updatedFrame },
                    onGestureListener = rememberOnGestureListener(
                        onSingleTapConfirmed = { motionEvent, _ ->
                            if (!isLoading) {
                                handleTap(
                                    motionEvent,
                                    frame,
                                    engine,
                                    modelLoader,
                                    materialLoader,
                                    childNodes
                                ) {
                                    isLoading = it
                                }
                            }
                        }
                    )
                )

                InfoText(
                    trackingFailureReason = trackingFailureReason,
                    childNodesEmpty = childNodes.isEmpty(),
                    errorMessage = errorMessage,
                    isLoading = isLoading
                )
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
        frame?.hitTest(motionEvent.x, motionEvent.y)
            ?.firstOrNull { it.isValid(depthPoint = false, point = false) }
            ?.createAnchorOrNull()?.let { anchor ->
                setLoading(true)
                lifecycleScope.launch {
                    try {
                        // Remove previous node if exists
                        (childNodes as MutableList<AnchorNode>).clear()

                        val anchorNode =
                            createAnchorNode(engine, modelLoader, materialLoader, anchor)
                        childNodes.add(anchorNode)
                    } catch (e: Exception) {
                        // Handle model loading error
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
        val modelNode = if (savedModelInstance != null) {
            ModelNode(
                modelInstance = savedModelInstance!!,
                scaleToUnits = 0.5f
            )
        } else {
            modelLoader.loadModelInstance(modelUrl)?.also {
                savedModelInstance = it
            }?.let { modelInstance ->
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = 0.5f
                )
            }
        } ?: throw IllegalStateException("Failed to load model")

        modelNode.apply {
            isEditable = true
            editableScaleRange = 0.2f..0.75f
            position = Position(0f, 0f, 0f)
        }

        anchorNode.addChildNode(modelNode)
        return anchorNode
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
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            color = Color.White,
            text = text
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center),
                color = Color.White
            )
        }
    }
}
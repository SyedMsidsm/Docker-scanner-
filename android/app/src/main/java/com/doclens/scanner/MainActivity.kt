package com.doclens.scanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.doclens.scanner.core.security.BiometricLockManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.doclens.scanner.core.designsystem.ScannerTheme
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.ui.capture.CaptureScreen
import com.doclens.scanner.ui.capture.CaptureViewModel
import com.doclens.scanner.ui.editor.EditorScreen
import com.doclens.scanner.ui.editor.EditorViewModel
import com.doclens.scanner.ui.export.ExportBottomSheet
import com.doclens.scanner.ui.export.ExportViewModel
import com.doclens.scanner.ui.gallery.ComponentGalleryScreen
import com.doclens.scanner.ui.library.LibraryScreen
import com.doclens.scanner.ui.library.LibraryViewModel
import com.doclens.scanner.ui.workflows.IdCopyWorkflowScreen
import com.doclens.scanner.ui.workflows.MergeSplitWorkflowScreen
import com.doclens.scanner.ui.workflows.WorkflowsScreen
import com.doclens.scanner.ui.ocr.OcrReviewScreen
import com.doclens.scanner.ui.ocr.OcrReviewViewModel
import com.doclens.scanner.ui.review.ReviewScreen
import com.doclens.scanner.ui.review.ReviewViewModel
import com.doclens.scanner.ui.settings.SettingsScreen
import com.doclens.scanner.ui.settings.SettingsViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ScannerApplication

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory())
            val settingsUiState by settingsViewModel.uiState.collectAsState()

            ScannerTheme(
                themeMode = settingsUiState.appearance.themeMode,
                textSize = settingsUiState.appearance.textSize
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ScannerTheme.colors.background
                ) {
                    ScannerNavHost(
                        activity = this@MainActivity,
                        app = app,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ScannerNavHost(
    activity: FragmentActivity,
    app: ScannerApplication,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "library"
    ) {
        // Library Screen (Home)
        composable("library") {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(app.repository)
            )

            LibraryScreen(
                viewModel = libraryViewModel,
                onStartScan = {
                    navController.navigate("capture")
                },
                onContinueSession = { sessionId ->
                    navController.navigate("capture?sessionId=$sessionId")
                },
                onReviewSession = { sessionId ->
                    navController.navigate("review?sessionId=$sessionId")
                },
                onOpenDocument = { documentId ->
                    navController.navigate("review?documentId=$documentId")
                },
                onImportGallery = {
                    navController.navigate("capture")
                },
                onImportPdf = {
                    navController.navigate("capture")
                },
                onOpenTools = {
                    navController.navigate("workflows")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onPromptUnlock = { documentId ->
                    BiometricLockManager.promptUnlock(
                        activity = activity,
                        title = "Unlock Scan",
                        onSuccess = {
                            navController.navigate("review?documentId=$documentId")
                        }
                    )
                }
            )
        }

        // Capture Screen
        composable(
            route = "capture?sessionId={sessionId}",
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            val captureViewModel: CaptureViewModel = viewModel(
                factory = CaptureViewModel.Factory(app.repository, sessionId)
            )

            CaptureScreen(
                viewModel = captureViewModel,
                onBack = { navController.popBackStack() },
                onFinishReview = { activeSessionId ->
                    navController.navigate("review?sessionId=$activeSessionId") {
                        popUpTo("library")
                    }
                }
            )
        }

        // Review Screen (Page Assembly, Reordering, Export)
        composable(
            route = "review?documentId={documentId}&sessionId={sessionId}",
            arguments = listOf(
                navArgument("documentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("documentId")
            val sessId = backStackEntry.arguments?.getString("sessionId")

            var exportDocumentId by remember { mutableStateOf<String?>(null) }

            val reviewViewModel: ReviewViewModel = viewModel(
                factory = ReviewViewModel.Factory(
                    repository = app.repository,
                    getAssetFile = { app.assetStore.getAssetFile(it) },
                    documentId = docId,
                    sessionId = sessId
                )
            )

            ReviewScreen(
                viewModel = reviewViewModel,
                onBack = {
                    navController.navigate("library") {
                        popUpTo("library") { inclusive = true }
                    }
                },
                onEditPage = { page ->
                    navController.navigate("editor/${page.id}/${page.documentId}")
                },
                onOcrPage = { page ->
                    navController.navigate("ocr/${page.id}/${page.documentId}")
                },
                onAddMorePages = { currentDocId ->
                    navController.navigate("capture")
                },
                onExportRequested = { currentDocId ->
                    exportDocumentId = currentDocId
                }
            )

            exportDocumentId?.let { id ->
                val exportViewModel: ExportViewModel = viewModel(
                    key = id,
                    factory = ExportViewModel.Factory(
                        repository = app.repository,
                        pdfEngine = app.pdfEngine,
                        assetStore = app.assetStore,
                        documentId = id
                    )
                )

                ExportBottomSheet(
                    viewModel = exportViewModel,
                    onDismiss = { exportDocumentId = null }
                )
            }
        }

        // Editor Screen (Crop, Perspective, Rotate, Filter, Adjustments)
        composable(
            route = "editor/{pageId}/{documentId}",
            arguments = listOf(
                navArgument("pageId") { type = NavType.StringType },
                navArgument("documentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: ""
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""

            val editorViewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.Factory(
                    repository = app.repository,
                    getAssetFile = { app.assetStore.getAssetFile(it) },
                    pageId = pageId
                )
            )

            // Initialize editor with page data
            val reviewViewModel: ReviewViewModel = viewModel(
                factory = ReviewViewModel.Factory(
                    repository = app.repository,
                    getAssetFile = { app.assetStore.getAssetFile(it) },
                    documentId = documentId,
                    sessionId = null
                )
            )
            val reviewState by reviewViewModel.uiState.collectAsState()
            val targetPage = reviewState.pages.find { it.id == pageId }
            if (targetPage != null) {
                editorViewModel.initFromPage(targetPage)
            }

            EditorScreen(
                viewModel = editorViewModel,
                imageEngine = app.imageEngine,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // OCR Review & Text Extraction Screen
        composable(
            route = "ocr/{pageId}/{documentId}",
            arguments = listOf(
                navArgument("pageId") { type = NavType.StringType },
                navArgument("documentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: ""
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""

            val ocrViewModel: OcrReviewViewModel = viewModel(
                factory = OcrReviewViewModel.Factory(
                    repository = app.repository,
                    ocrEngine = app.ocrEngine,
                    assetStore = app.assetStore,
                    pageId = pageId,
                    documentId = documentId
                )
            )

            OcrReviewScreen(
                viewModel = ocrViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Workflows Screen (Tools)
        composable("workflows") {
            WorkflowsScreen(
                onBack = { navController.popBackStack() },
                onOpenIdCopy = { navController.navigate("idcopy") },
                onOpenMergeSplit = { navController.navigate("merge_split") },
                onStartScan = { navController.navigate("capture") },
                onOpenSearch = { navController.navigate("library") }
            )
        }

        // Merge & Split Workflow Screen
        composable("merge_split") {
            MergeSplitWorkflowScreen(
                repository = app.repository,
                onBack = { navController.popBackStack() },
                onMerged = { newDocId ->
                    navController.navigate("review?documentId=$newDocId") {
                        popUpTo("library")
                    }
                },
                onSplitComplete = {
                    navController.popBackStack()
                }
            )
        }

        // ID Copy Workflow Screen
        composable("idcopy") {
            IdCopyWorkflowScreen(
                repository = app.repository,
                assetStore = app.assetStore,
                pdfEngine = app.pdfEngine,
                onBack = { navController.popBackStack() },
                onCompleted = { exportedPdf ->
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onOpenComponentGallery = { navController.navigate("gallery") }
            )
        }

        // Component Gallery Screen (Design System QA)
        composable("gallery") {
            ComponentGalleryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

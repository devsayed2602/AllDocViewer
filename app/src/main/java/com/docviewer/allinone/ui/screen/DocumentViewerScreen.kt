package com.docviewer.allinone.ui.screen

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.docviewer.allinone.ui.components.*
import com.docviewer.allinone.ui.viewmodel.*
import com.docviewer.allinone.viewer.CsvContent
import com.docviewer.allinone.viewer.ExcelContent
import com.docviewer.allinone.viewer.WordContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(uri: Uri, onBack: () -> Unit, viewModel: DocumentViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val currentBitmap by viewModel.currentBitmap.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val documentType by viewModel.documentType.collectAsState()
    var isControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(uri) { viewModel.loadDocument(uri) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { isControlsVisible = !isControlsVisible })
                }
        ) {
            // Document Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isControlsVisible) 64.dp else 0.dp,
                        bottom = if (isControlsVisible && (state is ViewerState.PdfLoaded || state is ViewerState.PptLoaded)) 80.dp else 0.dp
                    )
            ) {
                when (val currentState = state) {
                    is ViewerState.Loading -> LoadingOverlay()
                    is ViewerState.Error -> {
                        ErrorView(
                            message = currentState.message,
                            onRetry = { viewModel.loadDocument(uri) }
                        )
                    }
                    is ViewerState.PdfLoaded, is ViewerState.PptLoaded -> {
                        ZoomableBox { BitmapViewer(bitmap = currentBitmap) }
                    }
                    is ViewerState.WordLoaded -> WordContentView(content = currentState.content)
                    is ViewerState.ExcelLoaded -> ExcelContentView(content = currentState.content)
                    is ViewerState.TxtLoaded -> TxtContentView(content = currentState.content)
                    is ViewerState.CsvLoaded -> CsvContentView(content = currentState.content)
                    is ViewerState.ImageLoaded -> ZoomableBox { ImageContentView(uri = currentState.cachedUri) }
                }
            }

            // Top Bar Overlay
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FileTypeBadge(type = documentType)
                                if (state is ViewerState.PdfLoaded || state is ViewerState.PptLoaded) {
                                    Text(
                                        text = "Page ${currentPage + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }

            // Bottom Bar Overlay
            AnimatedVisibility(
                visible = isControlsVisible && (state is ViewerState.PdfLoaded || state is ViewerState.PptLoaded),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    if (state is ViewerState.PdfLoaded) {
                        val total = (state as ViewerState.PdfLoaded).pageCount
                        PageNavigation(currentPage, total, viewModel::previousPage, viewModel::nextPage)
                    } else if (state is ViewerState.PptLoaded) {
                        val total = (state as ViewerState.PptLoaded).slideCount
                        PageNavigation(currentPage, total, viewModel::previousPage, viewModel::nextPage)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageNavigation(
        currentPage: Int,
        totalPages: Int,
        onPrevious: () -> Unit,
        onNext: () -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onPrevious, enabled = currentPage > 0) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous page")
            }

            Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                        text = "${currentPage + 1} / $totalPages",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            FilledTonalIconButton(onClick = onNext, enabled = currentPage < totalPages - 1) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next page")
            }
        }
    }
}

@Composable
private fun BitmapViewer(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Document page",
                modifier =
                        Modifier.padding(8.dp)
                                .shadow(8.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
        )
    } else {
        CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WordContentView(content: WordContent) {
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                                }
                            },
            contentAlignment = Alignment.TopCenter
    ) {
        Surface(
                modifier = Modifier.widthIn(max = (800 * scale).dp).shadow(4.dp),
                color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = (24 * scale).dp),
                    contentPadding = PaddingValues(vertical = (24 * scale).dp),
                    verticalArrangement = Arrangement.spacedBy((16 * scale).dp)
            ) {
                items(content.paragraphs) { paragraph ->
                    val annotatedString = buildAnnotatedString {
                        withStyle(
                                style =
                                        SpanStyle(
                                                fontWeight =
                                                        if (paragraph.isBold || paragraph.isHeading)
                                                                FontWeight.Bold
                                                        else FontWeight.Normal,
                                                fontStyle =
                                                        if (paragraph.isItalic) FontStyle.Italic
                                                        else FontStyle.Normal,
                                                fontSize =
                                                        if (paragraph.isHeading) (24 * scale).sp
                                                        else (paragraph.fontSize * scale).sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                        ) { append(paragraph.text) }
                    }

                    Text(
                            text = annotatedString,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign =
                                    if (paragraph.isHeading) TextAlign.Center else TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcelContentView(content: ExcelContent) {
    var selectedSheet by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        if (content.sheets.size > 1) {
            ScrollableTabRow(
                    selectedTabIndex = selectedSheet,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp
            ) {
                content.sheets.forEachIndexed { index, sheet ->
                    Tab(
                            selected = selectedSheet == index,
                            onClick = { selectedSheet = index },
                            text = { Text(sheet.name, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }
        }

        val sheet = content.sheets.getOrNull(selectedSheet)
        if (sheet != null) {
            val scrollStateH = rememberScrollState()
            val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.2f, 5f)
                                        }
                                    }
            ) {
                val minWidth = (150 * scale).dp
                val maxWidth = (300 * scale).dp
                val padding = (12 * scale).dp
                val titleFontSize = (16 * scale).sp
                val titleLineHeight = (24 * scale).sp
                val bodyFontSize = (14 * scale).sp
                val bodyLineHeight = (20 * scale).sp

                androidx.compose.foundation.lazy.LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().horizontalScroll(scrollStateH)
                ) {
                    if (sheet.headers.isNotEmpty()) {
                        item {
                            Row(
                                    modifier =
                                            Modifier.background(
                                                    MaterialTheme.colorScheme.primaryContainer
                                            )
                            ) {
                                sheet.headers.forEach { header ->
                                    Box(
                                            modifier =
                                                    Modifier.widthIn(min = minWidth, max = maxWidth)
                                                            .border(
                                                                    0.5.dp,
                                                                    MaterialTheme.colorScheme
                                                                            .outlineVariant
                                                            )
                                                            .padding(padding)
                                    ) {
                                        Text(
                                                text = header,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontSize = titleFontSize,
                                                lineHeight = titleLineHeight,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(sheet.rows.size) { rowIndex ->
                        val row = sheet.rows[rowIndex]
                        Row(
                                modifier =
                                        Modifier.background(
                                                if (rowIndex % 2 == 0)
                                                        MaterialTheme.colorScheme.surface
                                                else
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.3f)
                                        )
                        ) {
                            val maxCols = maxOf(sheet.headers.size, row.size)
                            for (colIndex in 0 until maxCols) {
                                Box(
                                        modifier =
                                                Modifier.widthIn(min = minWidth, max = maxWidth)
                                                        .border(
                                                                0.5.dp,
                                                                MaterialTheme.colorScheme
                                                                        .outlineVariant
                                                        )
                                                        .padding(padding)
                                ) {
                                    Text(
                                            text = row.getOrElse(colIndex) { "" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = bodyFontSize,
                                            lineHeight = bodyLineHeight,
                                            color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CsvContentView(content: CsvContent) {
    var scale by remember { mutableFloatStateOf(1f) }
    val scrollStateH = rememberScrollState()
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.2f, 5f)
                                }
                            }
    ) {
        val minWidth = (120 * scale).dp
        val maxWidth = (400 * scale).dp
        val padding = (12 * scale).dp
        val titleFontSize = (16 * scale).sp
        val titleLineHeight = (24 * scale).sp
        val bodyFontSize = (14 * scale).sp
        val bodyLineHeight = (20 * scale).sp

        androidx.compose.foundation.lazy.LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().horizontalScroll(scrollStateH)
        ) {
            if (content.headers.isNotEmpty()) {
                item {
                    Row(
                            modifier =
                                    Modifier.background(MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        content.headers.forEach { header ->
                            Box(
                                    modifier =
                                            Modifier.widthIn(min = minWidth, max = maxWidth)
                                                    .border(
                                                            0.5.dp,
                                                            MaterialTheme.colorScheme.outlineVariant
                                                    )
                                                    .padding(padding)
                            ) {
                                Text(
                                        text = header,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = titleFontSize,
                                        lineHeight = titleLineHeight,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            items(content.rows.size) { rowIndex ->
                val row = content.rows[rowIndex]
                Row(
                        modifier =
                                Modifier.background(
                                        if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface
                                        else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.3f
                                                )
                                )
                ) {
                    val maxCols = maxOf(content.headers.size, row.size)
                    for (colIndex in 0 until maxCols) {
                        Box(
                                modifier =
                                        Modifier.widthIn(min = minWidth, max = maxWidth)
                                                .border(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.outlineVariant
                                                )
                                                .padding(padding)
                        ) {
                            Text(
                                    text = row.getOrElse(colIndex) { "" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = bodyFontSize,
                                    lineHeight = bodyLineHeight,
                                    color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TxtContentView(content: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                                }
                            }
    ) {
        Surface(
                modifier = Modifier.fillMaxSize().shadow(4.dp),
                color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .horizontalScroll(horizontalScrollState)
                                    .padding((24 * scale).dp)
            ) {
                Text(
                        text = content,
                        style =
                                MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily =
                                                androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                        fontSize = (14 * scale).sp,
                        lineHeight = (20 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ImageContentView(uri: Uri) {
    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
            contentAlignment = Alignment.Center
    ) {
        AsyncImage(
                model = uri,
                contentDescription = "Document Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    }
}

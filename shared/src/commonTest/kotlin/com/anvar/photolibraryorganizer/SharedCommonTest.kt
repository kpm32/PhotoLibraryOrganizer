package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportStorageSpace
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.detectMediaFileType
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.repository.StorageSpaceProvider
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.CheckImportStorageSpaceUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun scanRequiresSourceAndDestinationFolders() {
        val plan = PhotoLibraryPlan(
            sourceFolder = "/source",
            destinationFolder = null,
            importMode = ImportMode.ScanOnly,
        )

        assertFalse(plan.canScan)
    }

    @Test
    fun scanIsAvailableWhenBothFoldersAreSelected() {
        val plan = PhotoLibraryPlan(
            sourceFolder = "/source",
            destinationFolder = "/library",
            importMode = ImportMode.ScanOnly,
        )

        assertTrue(plan.canScan)
    }

    @Test
    fun scanOnlyIsDefaultSafeMode() {
        assertEquals(ImportMode.ScanOnly, ImportMode.entries.first())
    }

    @Test
    fun detectsImageFileTypeIgnoringCase() {
        val mediaType = detectMediaFileType("Summer.Photo.JPEG")

        assertEquals(MediaFileCategory.Image, mediaType?.category)
        assertEquals("jpeg", mediaType?.extension)
    }

    @Test
    fun detectsRawImageFileTypeIgnoringCase() {
        val mediaType = detectMediaFileType("Camera.NEF")

        assertEquals(MediaFileCategory.Image, mediaType?.category)
        assertEquals("nef", mediaType?.extension)
    }

    @Test
    fun detectsVideoFileTypeIgnoringCase() {
        val mediaType = detectMediaFileType("Family.MOV")

        assertEquals(MediaFileCategory.Video, mediaType?.category)
        assertEquals("mov", mediaType?.extension)
    }

    @Test
    fun detectsLegacyAndCameraVideoFileTypes() {
        listOf("clip.MPG", "camera.MTS", "stream.m2ts", "dvd.VOB", "old.WMV", "camera.MOD", "phone.3G2").forEach { fileName ->
            val mediaType = detectMediaFileType(fileName)

            assertEquals(MediaFileCategory.Video, mediaType?.category)
        }
    }

    @Test
    fun ignoresUnsupportedFileType() {
        assertEquals(null, detectMediaFileType("notes.txt"))
    }

    @Test
    fun ignoresAppleDoubleSidecarEvenWhenExtensionLooksLikeMedia() {
        assertEquals(null, detectMediaFileType("._IMG_0001.JPG"))
        assertEquals(null, detectMediaFileType("._video.mov"))
    }

    @Test
    fun scanUseCaseRejectsBlankSourceFolder() = runTest {
        val useCase = ScanSourceFolderUseCase(FakePhotoSourceScanner())

        val result = useCase(" ")

        val error = assertIs<AppResult.Error>(result)
        assertEquals(PhotoLibraryError.InvalidSourceFolder, error.error)
    }

    @Test
    fun scanUseCaseDelegatesValidSourceFolderToScanner() = runTest {
        val scanner = FakePhotoSourceScanner()
        val useCase = ScanSourceFolderUseCase(scanner)

        val result = useCase("/source")

        assertIs<AppResult.Success<ScanSourceFolderResult>>(result)
        assertEquals("/source", scanner.lastScannedPath)
    }

    @Test
    fun buildsTargetPathFromFileModifiedDate() {
        val useCase = BuildMediaFilePlanUseCase()

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = 1_735_689_600_000,
                ),
            ),
        )

        assertEquals(1, result.size)
        assertTrue(result.first().targetRelativePath.contains("/library-root/Library/2025/2025-01/"))
        assertTrue(result.first().targetRelativePath.endsWith("_IMG_0001.JPG"))
    }

    @Test
    fun buildsTargetPathFromCapturedDateWhenAvailable() {
        val useCase = BuildMediaFilePlanUseCase()

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = 1_735_689_600_000,
                    capturedAtEpochMillis = 1_577_836_800_000,
                ),
            ),
        )

        assertEquals(1, result.size)
        assertTrue(result.first().targetRelativePath.contains("/library-root/Library/2020/2020-01/"))
    }

    @Test
    fun givesCollidingPlanTargetsDistinctFileNames() {
        val useCase = BuildMediaFilePlanUseCase()
        val date = 1_735_689_600_000

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/camera-a/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1_024,
                    modifiedAtEpochMillis = date,
                ),
                ScannedMediaFile(
                    path = "/source/camera-b/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 2_048,
                    modifiedAtEpochMillis = date,
                ),
            ),
        )

        assertEquals(2, result.map { it.targetRelativePath }.toSet().size)
        assertTrue(result[1].targetRelativePath.endsWith("_IMG_0001 (2).JPG"))
    }

    @Test
    fun carriesContentHashIntoImportPlan() {
        val useCase = BuildMediaFilePlanUseCase()

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = 1_735_689_600_000,
                    contentHash = "abc123",
                ),
            ),
        )

        assertEquals("abc123", result.first().contentHash)
    }

    @Test
    fun carriesSourceDatesIntoImportPlan() {
        val useCase = BuildMediaFilePlanUseCase()
        val capturedAt = 1_577_836_800_000
        val modifiedAt = 1_735_689_600_000

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = modifiedAt,
                    capturedAtEpochMillis = capturedAt,
                ),
            ),
        )

        assertEquals(capturedAt, result.first().capturedAtEpochMillis)
        assertEquals(modifiedAt, result.first().modifiedAtEpochMillis)
    }

    @Test
    fun buildsTargetPathFromConfiguredImportRules() {
        val useCase = BuildMediaFilePlanUseCase()

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = 1_735_689_600_000,
                ),
            ),
            importRules = ImportOrganizationRules(
                libraryFolderName = "Archive",
                folderTemplate = "YYYY/MM",
                fileNameTemplate = "YYYYMMDD-HHmmss-original-name.ext",
            ),
        )

        assertTrue(result.first().targetRelativePath.contains("/library-root/Archive/2025/01/"))
        assertTrue(Regex(""".*/20250101-\d{6}-IMG_0001\.JPG$""").matches(result.first().targetRelativePath))
    }

    @Test
    fun importIsUnavailableBeforeScanPlanExists() {
        val useCase = ResolveImportAvailabilityUseCase()

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = emptyList(),
        )

        assertIs<ImportAvailability.Unavailable>(result)
    }

    @Test
    fun importIsUnavailableInScanOnlyMode() {
        val useCase = ResolveImportAvailabilityUseCase()

        val result = useCase(
            importMode = ImportMode.ScanOnly,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertIs<ImportAvailability.Unavailable>(result)
    }

    @Test
    fun importIsAvailableForCopyModeWhenPlanExists() {
        val useCase = ResolveImportAvailabilityUseCase()

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertEquals(ImportAvailability.Available, result)
    }

    @Test
    fun storageCheckReportsAvailableSpaceForFilesThatWillBeCopied() = runTest {
        val useCase = CheckImportStorageSpaceUseCase(FakeStorageSpaceProvider(3_000))

        val result = useCase(
            destinationFolder = "/library",
            plannedFiles = listOf(
                fakePlannedMediaFile().copy(sizeBytes = 1_000),
                fakePlannedMediaFile().copy(sizeBytes = 2_000, targetStatus = ImportTargetStatus.AlreadyExists),
            ),
        )

        assertEquals(ImportStorageSpace.Available(requiredBytes = 1_000, availableBytes = 3_000), result)
    }

    @Test
    fun storageCheckBlocksCopyWhenDestinationSpaceIsInsufficient() = runTest {
        val useCase = CheckImportStorageSpaceUseCase(FakeStorageSpaceProvider(999))

        val result = useCase(
            destinationFolder = "/library",
            plannedFiles = listOf(fakePlannedMediaFile().copy(sizeBytes = 1_000)),
        )

        assertEquals(ImportStorageSpace.Insufficient(requiredBytes = 1_000, availableBytes = 999), result)
    }

    @Test
    fun importUseCaseDelegatesCopyModeToImporter() = runTest {
        val importer = FakeMediaFileImporter()
        val useCase = ImportMediaFilesUseCase(importer)

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(1, importer.lastPlannedFiles.size)
    }

    @Test
    fun importUseCaseDelegatesMoveModeToImporter() = runTest {
        val importer = FakeMediaFileImporter()
        val useCase = ImportMediaFilesUseCase(importer)

        val result = useCase(
            importMode = ImportMode.Move,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(1, importer.lastMovedFiles.size)
    }

    @Test
    fun importUseCaseSkipsAlreadyExistingTargetsBeforeImporter() = runTest {
        val importer = FakeMediaFileImporter()
        val useCase = ImportMediaFilesUseCase(importer)

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = listOf(
                fakePlannedMediaFile().copy(targetStatus = ImportTargetStatus.Ready),
                fakePlannedMediaFile().copy(
                    sourcePath = "/source/existing.jpg",
                    targetStatus = ImportTargetStatus.AlreadyExists,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(1, importer.lastPlannedFiles.size)
        assertEquals(1, success.data.copiedFiles)
        assertEquals(1, success.data.skippedFiles)
    }

    @Test
    fun importUseCaseReturnsSkippedResultWhenEverythingAlreadyExists() = runTest {
        val importer = FakeMediaFileImporter()
        val useCase = ImportMediaFilesUseCase(importer)

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = listOf(fakePlannedMediaFile().copy(targetStatus = ImportTargetStatus.AlreadyExists)),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(0, importer.lastPlannedFiles.size)
        assertEquals(0, success.data.copiedFiles)
        assertEquals(1, success.data.skippedFiles)
        assertEquals(0, success.data.failedFiles)
    }

    @Test
    fun appStateKeepsSelectionWhenApplyingUpdatedLibraryIndex() {
        val selected = fakePlannedMediaFile().copy(sourcePath = "/library/selected.jpg")
        val other = fakePlannedMediaFile().copy(sourcePath = "/library/other.jpg")
        val state = PhotoLibraryAppState().apply {
            selectedFile = selected
        }

        state.applyLibraryIndexSnapshot(
            snapshot = fakeLibraryIndexSnapshot(libraryFiles = listOf(other, selected)),
            selectFirstFile = false,
        )

        assertEquals(selected, state.selectedFile)
    }

    @Test
    fun appStateRemovesFileAndSelectsNextPreferredFile() {
        val removed = fakePlannedMediaFile().copy(sourcePath = "/library/removed.jpg")
        val next = fakePlannedMediaFile().copy(sourcePath = "/library/next.jpg")
        val state = PhotoLibraryAppState().apply {
            libraryFiles = listOf(removed, next)
            selectedFile = removed
        }

        state.removeFileFromVisibleState(
            path = removed.sourcePath,
            preferredSelectionFiles = listOf(removed, next),
        )

        assertEquals(listOf(next), state.libraryFiles)
        assertEquals(next, state.selectedFile)
    }

    @Test
    fun fileActionsOpenLibrarySubfolderThroughRevealHandler() {
        val revealHandler = FakeFileRevealHandler()
        val issues = mutableListOf<Pair<String, String>>()
        val actions = PhotoLibraryFileActions(
            fileRevealHandler = revealHandler,
            addIssue = { title, detail -> issues += title to detail },
        )

        actions.openDuplicatesFolder("/library-root/")

        assertEquals("/library-root/Duplicates", revealHandler.lastOpenedPath)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun fileActionsReportOpenFailureAsIssue() {
        val revealHandler = FakeFileRevealHandler(openResult = false)
        val issues = mutableListOf<Pair<String, String>>()
        val actions = PhotoLibraryFileActions(
            fileRevealHandler = revealHandler,
            addIssue = { title, detail -> issues += title to detail },
        )

        actions.openSelectedFile(fakePlannedMediaFile().copy(sourcePath = "/library/missing.jpg"))

        assertEquals("/library/missing.jpg", revealHandler.lastOpenedPath)
        assertEquals(1, issues.size)
        assertTrue(issues.first().second.contains("/library/missing.jpg"))
    }

    @Test
    fun libraryActionsLoadIndexAppliesSnapshot() = runTest {
        val indexedFile = fakePlannedMediaFile().copy(sourcePath = "/library/indexed.jpg")
        val state = PhotoLibraryAppState()
        val actions = fakeLibraryActions(
            state = state,
            coroutineScope = this,
            libraryIndexStorage = FakeLibraryIndexStorage(
                snapshot = fakeLibraryIndexSnapshot(libraryFiles = listOf(indexedFile)),
            ),
        )

        val loaded = actions.loadLibraryIndexIfAvailable("/library")

        assertTrue(loaded)
        assertEquals(listOf(indexedFile), state.libraryFiles)
        assertEquals(indexedFile, state.selectedFile)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun libraryActionsChooseDestinationSavesSettingsAndLoadsIndex() = runTest {
        val indexedFile = fakePlannedMediaFile().copy(sourcePath = "/library/from-index.jpg")
        val state = PhotoLibraryAppState().apply {
            sourceFolder = "/source"
        }
        val settingsStorage = FakeAppSettingsStorage()
        val actions = fakeLibraryActions(
            state = state,
            coroutineScope = this,
            appSettingsStorage = settingsStorage,
            folderPicker = FakeFolderPicker("/library"),
            libraryIndexStorage = FakeLibraryIndexStorage(
                snapshot = fakeLibraryIndexSnapshot(libraryFiles = listOf(indexedFile)),
            ),
        )

        actions.chooseDestinationFolder()
        advanceUntilIdle()

        assertEquals("/library", state.destinationFolder)
        assertEquals("/library", settingsStorage.lastSavedSettings?.destinationFolder)
        assertEquals(listOf(indexedFile), state.libraryFiles)
    }

    private fun fakePlannedMediaFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/source/IMG_0001.JPG",
            fileName = "IMG_0001.JPG",
            targetRelativePath = "/library/Library/2025/2025-01/IMG_0001.JPG",
            sizeBytes = 1024,
        )
    }

    private fun fakeLibraryActions(
        state: PhotoLibraryAppState = PhotoLibraryAppState(),
        coroutineScope: CoroutineScope,
        photoSourceScanner: PhotoSourceScanner = FakePhotoSourceScanner(),
        libraryIndexStorage: LibraryIndexStorage = FakeLibraryIndexStorage(),
        appSettingsStorage: AppSettingsStorage = FakeAppSettingsStorage(),
        folderPicker: FolderPicker = FakeFolderPicker(null),
    ): PhotoLibraryLibraryActions {
        return PhotoLibraryLibraryActions(
            appState = state,
            photoSourceScanner = photoSourceScanner,
            libraryIndexStorage = libraryIndexStorage,
            appSettingsStorage = appSettingsStorage,
            folderPicker = folderPicker,
            coroutineScope = coroutineScope,
        )
    }

    private class FakeFileRevealHandler(
        private val openResult: Boolean = true,
        private val revealResult: Boolean = true,
    ) : FileRevealHandler {
        var lastOpenedPath: String? = null
        var lastRevealedPath: String? = null

        override fun open(path: String): Boolean {
            lastOpenedPath = path
            return openResult
        }

        override fun reveal(path: String): Boolean {
            lastRevealedPath = path
            return revealResult
        }
    }

    private class FakeFolderPicker(
        private val folder: String?,
    ) : FolderPicker {
        override fun chooseFolder(title: String): String? = folder
    }

    private class FakeAppSettingsStorage : AppSettingsStorage {
        var lastSavedSettings: AppSettings? = null

        override suspend fun loadSettings(): AppSettings = AppSettings()

        override suspend fun saveSettings(settings: AppSettings) {
            lastSavedSettings = settings
        }
    }

    private class FakeLibraryIndexStorage(
        private val snapshot: LibraryIndexSnapshot? = null,
    ) : LibraryIndexStorage {
        var lastLoadedDestination: String? = null
        var lastSavedSnapshot: LibraryIndexSnapshot? = null

        override suspend fun load(destinationFolder: String?): LibraryIndexSnapshot? {
            lastLoadedDestination = destinationFolder
            return snapshot
        }

        override suspend fun save(snapshot: LibraryIndexSnapshot) {
            lastSavedSnapshot = snapshot
        }
    }

    private fun fakeLibraryIndexSnapshot(
        libraryFiles: List<PlannedMediaFile> = emptyList(),
        duplicateFiles: List<PlannedMediaFile> = emptyList(),
        unsupportedFiles: List<PlannedMediaFile> = emptyList(),
    ): LibraryIndexSnapshot {
        return LibraryIndexSnapshot(
            destinationFolder = "/library",
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
            unsupportedFiles = unsupportedFiles,
            updatedAtEpochMillis = 0,
        )
    }

    private class FakePhotoSourceScanner : PhotoSourceScanner {
        var lastScannedPath: String? = null

        override suspend fun scanFolder(
            path: String,
            onProgress: (ScanSourceFolderProgress) -> Unit,
            readContentHash: Boolean,
        ): AppResult<ScanSourceFolderResult> {
            lastScannedPath = path
            return AppResult.Success(
                ScanSourceFolderResult(
                    sourceFolder = path,
                    mediaFiles = emptyList(),
                    summary = ScanSourceFolderSummary(
                        scannedFiles = 0,
                        mediaFiles = 0,
                        imageFiles = 0,
                        videoFiles = 0,
                        capturedDateFiles = 0,
                        unsupportedFiles = 0,
                        totalMediaBytes = 0,
                    ),
                ),
            )
        }
    }

    private class FakeStorageSpaceProvider(
        private val availableBytes: Long?,
    ) : StorageSpaceProvider {
        override suspend fun availableBytes(path: String): Long? = availableBytes
    }

    private class FakeMediaFileImporter : MediaFileImporter {
        var lastPlannedFiles: List<PlannedMediaFile> = emptyList()
        var lastMovedFiles: List<PlannedMediaFile> = emptyList()

        override suspend fun copyFiles(
            plannedFiles: List<PlannedMediaFile>,
            onProgress: (ImportMediaFilesProgress) -> Unit,
        ): AppResult<ImportMediaFilesResult> {
            lastPlannedFiles = plannedFiles
            return AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = plannedFiles.size,
                    skippedFiles = 0,
                    failedFiles = 0,
                ),
            )
        }

        override suspend fun moveFiles(
            plannedFiles: List<PlannedMediaFile>,
            onProgress: (ImportMediaFilesProgress) -> Unit,
        ): AppResult<ImportMediaFilesResult> {
            lastMovedFiles = plannedFiles
            return AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = 0,
                    movedFiles = plannedFiles.size,
                    skippedFiles = 0,
                    failedFiles = 0,
                ),
            )
        }
    }
}

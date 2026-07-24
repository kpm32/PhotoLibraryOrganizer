package com.anvar.photolibraryorganizer

import androidx.compose.runtime.Composable
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportUnavailableReason
import com.anvar.photolibraryorganizer.presentation.AppSection
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import photolibraryorganizer.shared.generated.resources.Res
import photolibraryorganizer.shared.generated.resources.import_mode_copy_description
import photolibraryorganizer.shared.generated.resources.import_mode_copy_title
import photolibraryorganizer.shared.generated.resources.import_mode_move_description
import photolibraryorganizer.shared.generated.resources.import_mode_move_title
import photolibraryorganizer.shared.generated.resources.import_mode_scan_only_description
import photolibraryorganizer.shared.generated.resources.import_mode_scan_only_title
import photolibraryorganizer.shared.generated.resources.import_unavailable_plan_missing
import photolibraryorganizer.shared.generated.resources.import_unavailable_scan_only
import photolibraryorganizer.shared.generated.resources.section_about
import photolibraryorganizer.shared.generated.resources.section_all_photos
import photolibraryorganizer.shared.generated.resources.section_duplicates
import photolibraryorganizer.shared.generated.resources.section_errors
import photolibraryorganizer.shared.generated.resources.section_import
import photolibraryorganizer.shared.generated.resources.section_months
import photolibraryorganizer.shared.generated.resources.section_unsupported
import photolibraryorganizer.shared.generated.resources.section_unsupported_nav
import photolibraryorganizer.shared.generated.resources.section_without_date
import photolibraryorganizer.shared.generated.resources.section_years

@Composable
internal fun AppSection.titleText(): String {
    return stringResource(titleResource())
}

@Composable
internal fun AppSection.navigationTitleText(): String {
    return stringResource(navigationTitleResource())
}

@Composable
internal fun ImportMode.titleText(): String {
    return stringResource(titleResource())
}

@Composable
internal fun ImportMode.descriptionText(): String {
    return stringResource(descriptionResource())
}

@Composable
internal fun ImportAvailability.Unavailable.reasonText(): String {
    return stringResource(reason.resource())
}

private fun AppSection.titleResource(): StringResource {
    return when (this) {
        AppSection.AllPhotos -> Res.string.section_all_photos
        AppSection.Years -> Res.string.section_years
        AppSection.Months -> Res.string.section_months
        AppSection.WithoutDate -> Res.string.section_without_date
        AppSection.Import -> Res.string.section_import
        AppSection.Duplicates -> Res.string.section_duplicates
        AppSection.Unsupported -> Res.string.section_unsupported
        AppSection.Errors -> Res.string.section_errors
        AppSection.About -> Res.string.section_about
    }
}

private fun AppSection.navigationTitleResource(): StringResource {
    return when (this) {
        AppSection.Unsupported -> Res.string.section_unsupported_nav
        else -> titleResource()
    }
}

private fun ImportMode.titleResource(): StringResource {
    return when (this) {
        ImportMode.ScanOnly -> Res.string.import_mode_scan_only_title
        ImportMode.Copy -> Res.string.import_mode_copy_title
        ImportMode.Move -> Res.string.import_mode_move_title
    }
}

private fun ImportMode.descriptionResource(): StringResource {
    return when (this) {
        ImportMode.ScanOnly -> Res.string.import_mode_scan_only_description
        ImportMode.Copy -> Res.string.import_mode_copy_description
        ImportMode.Move -> Res.string.import_mode_move_description
    }
}

private fun ImportUnavailableReason.resource(): StringResource {
    return when (this) {
        ImportUnavailableReason.PlanMissing -> Res.string.import_unavailable_plan_missing
        ImportUnavailableReason.ScanOnlyMode -> Res.string.import_unavailable_scan_only
    }
}

/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.Constants.SHARE_URL
import com.aurora.extensions.appInfo
import com.aurora.extensions.browse
import com.aurora.extensions.share
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppListComposable
import com.aurora.store.compose.composables.app.AppProgressComposable
import com.aurora.store.compose.composables.app.NoAppComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.compose.menu.AppDetailsMenu
import com.aurora.store.compose.menu.items.AppDetailsMenuItem
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.ui.details.components.AppActions
import com.aurora.store.compose.ui.details.components.AppChangelog
import com.aurora.store.compose.ui.details.components.AppCompatibility
import com.aurora.store.compose.ui.details.components.AppDataSafety
import com.aurora.store.compose.ui.details.components.AppDetails
import com.aurora.store.compose.ui.details.components.AppDeveloperDetails
import com.aurora.store.compose.ui.details.components.AppPrivacy
import com.aurora.store.compose.ui.details.components.AppRatingAndReviews
import com.aurora.store.compose.ui.details.components.AppScreenshots
import com.aurora.store.compose.ui.details.components.AppTags
import com.aurora.store.compose.ui.details.components.AppTesting
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.AppState
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PackageUtil.PACKAGE_NAME_GMS
import com.aurora.store.util.ShortcutManagerUtil
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@Composable
fun AppDetailsScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val app by viewModel.app.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val featuredReviews by viewModel.featuredReviews.collectAsStateWithLifecycle()
    val favorite by viewModel.favourite.collectAsStateWithLifecycle()
    val exodusReport by viewModel.exodusReport.collectAsStateWithLifecycle()
    val dataSafetyReport by viewModel.dataSafetyReport.collectAsStateWithLifecycle()
    val plexusScores by viewModel.plexusScores.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) { viewModel.fetchAppDetails(packageName) }

    with(app) {
        when {
            this != null -> {
                if (this.packageName.isBlank()) {
                    ScreenContentLoading(onNavigateUp = onNavigateUp)
                } else {
                    ScreenContentApp(
                        app = this,
                        featuredReviews = featuredReviews,
                        suggestions = suggestions,
                        isFavorite = favorite,
                        isAnonymous = viewModel.authProvider.isAnonymous,
                        state = state,
                        plexusScores = plexusScores,
                        dataSafetyReport = dataSafetyReport,
                        exodusReport = exodusReport,
                        onNavigateUp = onNavigateUp,
                        onNavigateToAppDetails = onNavigateToAppDetails,
                        onDownload = { viewModel.purchase(this) },
                        onFavorite = { viewModel.toggleFavourite(this) },
                        onCancelDownload = { viewModel.cancelDownload(this) },
                        onUninstall = { AppInstaller.uninstall(context, packageName) },
                        onOpen = {
                            try {
                                context.startActivity(
                                    PackageUtil.getLaunchIntent(context, packageName)
                                )
                            } catch (exception: ActivityNotFoundException) {
                                context.toast(context.getString(R.string.unable_to_open))
                            }
                        },
                        onTestingSubscriptionChange = { subscribe ->
                            viewModel.updateTestingProgramStatus(packageName, subscribe)
                        }
                    )
                }
            }

            // TODO: Deal with different kind of errors
            else -> ScreenContentError(onNavigateUp = onNavigateUp)
        }
    }
}

/**
 * Composable to show progress while fetching app details
 */
@Composable
private fun ScreenContentLoading(onNavigateUp: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBarComposable(onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        AppProgressComposable(modifier = Modifier.padding(paddingValues))
    }
}

/**
 * Composable to display errors related to fetching app details
 */
@Composable
private fun ScreenContentError(onNavigateUp: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBarComposable(onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        NoAppComposable(
            modifier = Modifier.padding(paddingValues),
            icon = R.drawable.ic_apps_outage,
            message = R.string.toast_app_unavailable
        )
    }
}

/**
 * Composable to display app details and suggestions
 */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ScreenContentApp(
    app: App,
    featuredReviews: List<Review> = emptyList(),
    suggestions: List<App> = emptyList(),
    isFavorite: Boolean = false,
    isAnonymous: Boolean = true,
    state: AppState = AppState.Unavailable,
    plexusScores: Scores? = null,
    dataSafetyReport: DataSafetyReport? = null,
    exodusReport: Report? = null,
    onNavigateUp: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    onDownload: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onOpen: () -> Unit = {},
    onTestingSubscriptionChange: (subscribe: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator<Screen>()
    val coroutineScope = rememberCoroutineScope()
    val shouldShowMenuOnMainPane = scaffoldNavigator
        .scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden

    fun showMainPane() {
        coroutineScope.launch {
            scaffoldNavigator.navigateBack()
        }
    }

    fun showExtraPane(screen: Screen) {
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, screen)
        }
    }

    @Composable
    fun SetupMenu() {
        AppDetailsMenu(isInstalled = app.isInstalled, isFavorite = isFavorite) { menuItem ->
            when (menuItem) {
                AppDetailsMenuItem.FAVORITE -> onFavorite()
                AppDetailsMenuItem.MANUAL_DOWNLOAD -> {
                    showExtraPane(Screen.DetailsManualDownload)
                }
                AppDetailsMenuItem.SHARE -> context.share(app)
                AppDetailsMenuItem.APP_INFO -> context.appInfo(app.packageName)
                AppDetailsMenuItem.PLAY_STORE -> context.browse("$SHARE_URL${app.packageName}")
                AppDetailsMenuItem.ADD_TO_HOME -> {
                    ShortcutManagerUtil.requestPinShortcut(context, app.packageName)
                }
            }
        }
    }

    NavigableSupportingPaneScaffold(
        navigator = scaffoldNavigator,
        mainPane = {
            AnimatedPane {
                ScreenContentAppMainPane(
                    app = app,
                    featuredReviews = featuredReviews,
                    state = state,
                    isAnonymous = isAnonymous,
                    plexusScores = plexusScores,
                    dataSafetyReport = dataSafetyReport,
                    exodusReport = exodusReport,
                    onNavigateUp = onNavigateUp,
                    onNavigateToScreen = { screen -> showExtraPane(screen) },
                    onDownload = onDownload,
                    onCancelDownload = onCancelDownload,
                    onUninstall = onUninstall,
                    onOpen = onOpen,
                    onTestingSubscriptionChange = onTestingSubscriptionChange,
                    menuActions = { if (shouldShowMenuOnMainPane) SetupMenu() }
                )
            }
        },
        supportingPane = {
            AnimatedPane {
                ScreenContentAppSupportingPane(
                    suggestions = suggestions,
                    onNavigateToAppDetails = onNavigateToAppDetails,
                    menuActions = { if (!shouldShowMenuOnMainPane) SetupMenu() }
                )
            }
        },
        extraPane = {
            scaffoldNavigator.currentDestination?.contentKey?.let { screen ->
                AnimatedPane {
                    when (screen) {
                        is Screen.DetailsReview -> DetailsReviewScreen(onNavigateUp = ::showMainPane)
                        is Screen.DetailsExodus -> DetailsExodusScreen(onNavigateUp = ::showMainPane)
                        is Screen.DetailsMore -> DetailsMoreScreen(
                            onNavigateUp = ::showMainPane,
                            onNavigateToAppDetails = onNavigateToAppDetails
                        )
                        is Screen.DetailsPermission -> DetailsPermissionScreen(
                            onNavigateUp = ::showMainPane
                        )
                        is Screen.DetailsScreenshot -> DetailsScreenshotScreen(
                            index = screen.index,
                            onNavigateUp = ::showMainPane
                        )
                        is Screen.DetailsManualDownload -> DetailsManualDownloadScreen(
                            onNavigateUp = ::showMainPane
                        )
                        is Screen.DevProfile -> DevProfileScreen(
                            publisherId = app.developerName,
                            onNavigateUp = ::showMainPane,
                            onNavigateToAppDetails = { onNavigateToAppDetails(it) }
                        )

                        else -> {}
                    }
                }
            }
        }
    )
}

/**
 * Composable to display app details
 */
@Composable
private fun ScreenContentAppMainPane(
    app: App,
    featuredReviews: List<Review> = emptyList(),
    state: AppState = AppState.Unavailable,
    isAnonymous: Boolean,
    plexusScores: Scores?,
    dataSafetyReport: DataSafetyReport?,
    exodusReport: Report?,
    onNavigateUp: () -> Unit,
    onNavigateToScreen: (screen: Screen) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: () -> Unit,
    onTestingSubscriptionChange: (subscribe: Boolean) -> Unit,
    menuActions: @Composable (RowScope.() -> Unit) = {}
) {

    @Composable
    fun SetupAppActions() {
        when (state) {
            is AppState.Downloading -> {
                AppActions(
                    primaryActionDisplayName = stringResource(R.string.action_open),
                    secondaryActionDisplayName = stringResource(R.string.action_cancel),
                    isPrimaryActionEnabled = false,
                    onSecondaryAction = onCancelDownload
                )
            }

            is AppState.Updatable -> {
                AppActions(
                    primaryActionDisplayName = stringResource(R.string.action_update),
                    secondaryActionDisplayName = stringResource(R.string.action_uninstall),
                    onPrimaryAction = onDownload,
                    onSecondaryAction = onUninstall
                )
            }

            is AppState.Installed -> {
                AppActions(
                    primaryActionDisplayName = stringResource(R.string.action_open),
                    secondaryActionDisplayName = stringResource(R.string.action_uninstall),
                    onPrimaryAction = onOpen,
                    onSecondaryAction = onUninstall
                )
            }

            else -> {
                val primaryActionName = if (state is AppState.Archived) {
                    stringResource(R.string.action_unarchive)
                } else {
                    if (app.isFree) stringResource(R.string.action_install) else app.price
                }

                AppActions(
                    primaryActionDisplayName = primaryActionName,
                    secondaryActionDisplayName = stringResource(R.string.title_manual_download),
                    onPrimaryAction = onDownload,
                    onSecondaryAction = { onNavigateToScreen(Screen.DetailsManualDownload) }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBarComposable(onNavigateUp = onNavigateUp, actions = menuActions)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            AppDetails(
                app = app,
                inProgress = state.inProgress(),
                progress = state.progress(),
                onNavigateToDetailsDevProfile = { onNavigateToScreen(Screen.DevProfile(it)) },
                isUpdatable = state is AppState.Updatable
            )

            SetupAppActions()

            AppTags(app = app)
            AppChangelog(changelog = app.changes)
            HeaderComposable(
                title = stringResource(R.string.details_more_about_app),
                subtitle = app.shortDescription,
                onClick = { onNavigateToScreen(Screen.DetailsMore) }
            )

            AppScreenshots(
                screenshots = app.screenshots,
                onNavigateToScreenshot = { onNavigateToScreen(Screen.DetailsScreenshot(it)) }
            )

            AppRatingAndReviews(
                rating = app.rating,
                featuredReviews = featuredReviews,
                onNavigateToDetailsReview = { onNavigateToScreen(Screen.DetailsReview) }
            )

            if (!isAnonymous && app.testingProgram?.isAvailable == true) {
                AppTesting(
                    isSubscribed = app.testingProgram!!.isSubscribed,
                    onTestingSubscriptionChange = onTestingSubscriptionChange
                )
            }

            AppCompatibility(
                needsGms = app.dependencies.dependentPackages.contains(PACKAGE_NAME_GMS),
                plexusScores = plexusScores
            )

            HeaderComposable(
                title = stringResource(R.string.details_permission),
                subtitle = if (app.permissions.isNotEmpty()) {
                    stringResource(R.string.permissions_requested, app.permissions.size)
                } else {
                    stringResource(R.string.details_no_permission)
                },
                onClick = if (app.permissions.isNotEmpty()) {
                    { onNavigateToScreen(Screen.DetailsPermission) }
                } else {
                    null
                }
            )

            if (dataSafetyReport != null) {
                AppDataSafety(
                    report = dataSafetyReport,
                    privacyPolicyUrl = app.privacyPolicyUrl
                )
            }

            AppPrivacy(
                report = exodusReport,
                onNavigateToDetailsExodus = if (!exodusReport?.trackers.isNullOrEmpty()) {
                    { onNavigateToScreen(Screen.DetailsExodus) }
                } else {
                    null
                }
            )

            AppDeveloperDetails(
                address = app.developerAddress,
                website = app.developerWebsite,
                email = app.developerEmail
            )
        }
    }
}

/**
 * Composable to display similar and related app suggestions
 */
@Composable
private fun ScreenContentAppSupportingPane(
    menuActions: @Composable (RowScope.() -> Unit) = {},
    suggestions: List<App> = emptyList(),
    onNavigateToAppDetails: (packageName: String) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBarComposable(actions = menuActions) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.margin_medium)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_suggestions),
                    contentDescription = null
                )
                HeaderComposable(title = stringResource(R.string.pref_ui_similar_apps))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.padding_medium))
            ) {
                items(items = suggestions, key = { item -> item.id }) { app ->
                    AppListComposable(
                        app = app,
                        onClick = { onNavigateToAppDetails(app.packageName) }
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppDetailsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContentApp(
            app = app,
            isAnonymous = false,
            suggestions = List(10) { app.copy(id = Random.nextInt()) }
        )
    }
}

@Preview
@Composable
private fun AppDetailsScreenPreviewLoading() {
    ScreenContentLoading()
}

@Preview
@Composable
private fun AppDetailsScreenPreviewError() {
    ScreenContentError()
}

/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.ErrorComposable
import com.aurora.store.compose.composables.ProgressComposable
import com.aurora.store.compose.composables.app.AppListComposable
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.viewmodel.search.SearchViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SearchScreen(onNavigateUp: () -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val results = viewModel.apps.collectAsLazyPagingItems()

    ScreenContent(
        suggestions = suggestions,
        results = results,
        onNavigateUp = onNavigateUp
    )
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ScreenContent(
    suggestions: List<String> = emptyList(),
    results: LazyPagingItems<App> = flowOf(PagingData.empty<App>()).collectAsLazyPagingItems(),
    onNavigateUp: () -> Unit = {}
) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    fun showDetailPane(packageName: String) {
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, packageName)
        }
    }

    @Composable
    fun ListPane() {
        Scaffold(
            topBar = {
                Box(modifier = Modifier.fillMaxWidth()) {

                }
            }
        ) { paddingValues ->
            when (results.loadState.refresh) {
                is LoadState.Loading -> ProgressComposable()

                is LoadState.Error -> {
                    ErrorComposable(
                        modifier = Modifier.padding(paddingValues),
                        icon = R.drawable.ic_disclaimer,
                        message = R.string.error
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .padding(vertical = dimensionResource(R.dimen.padding_medium))
                    ) {
                        items(count = results.itemCount, key = results.itemKey { it.id }) { index ->
                            results[index]?.let { app ->
                                AppListComposable(
                                    app = app,
                                    onClick = { showDetailPane(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DetailPane() {
        scaffoldNavigator.currentDestination?.contentKey?.let { packageName ->
            AppDetailsScreen(
                packageName = packageName,
                onNavigateUp = onNavigateUp,
                onNavigateToAppDetails = {}
            )
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = { AnimatedPane { ListPane() } },
        detailPane = { AnimatedPane { DetailPane() } }
    )
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun SearchScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(10) { app.copy(id = Random.nextInt()) }
    val results = flowOf(PagingData.from(apps)).collectAsLazyPagingItems()

    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(results = results)
    }
}

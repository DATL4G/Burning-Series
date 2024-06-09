package dev.datlag.burningseries.ui.navigation.screen.medium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import dev.chrisbanes.haze.HazeState
import dev.datlag.burningseries.LocalHaze
import dev.datlag.burningseries.model.Series
import dev.datlag.burningseries.model.SeriesData
import dev.datlag.burningseries.network.EpisodeStateMachine
import dev.datlag.burningseries.network.SeriesStateMachine
import dev.datlag.burningseries.network.state.EpisodeAction
import dev.datlag.burningseries.network.state.EpisodeState
import dev.datlag.burningseries.network.state.SeriesState
import dev.datlag.skeo.Stream
import dev.datlag.tooling.compose.ioDispatcher
import dev.datlag.tooling.compose.withMainContext
import dev.datlag.tooling.decompose.ioScope
import dev.datlag.tooling.safeCast
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import org.kodein.di.DI
import org.kodein.di.instance

class MediumScreenComponent(
    componentContext: ComponentContext,
    override val di: DI,
    private val initialSeriesData: SeriesData,
    override val initialIsAnime: Boolean,
    private val onBack: () -> Unit,
    private val onWatch: (Series.Episode, ImmutableCollection<Stream>) -> Unit
) : MediumComponent, ComponentContext by componentContext {

    private val seriesStateMachine by instance<SeriesStateMachine>()
    override val seriesState: StateFlow<SeriesState> = seriesStateMachine.state.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = seriesStateMachine.currentState
    )

    private val successState = seriesState.mapNotNull {
        it.safeCast<SeriesState.Success>().also { success ->
            seriesData = success?.series ?: seriesData
        }
    }

    override var seriesData: SeriesData = initialSeriesData
        private set

    override val seriesTitle: Flow<String> = successState.map { it.series.mainTitle }
    override val seriesSubTitle: Flow<String?> = successState.map { it.series.subTitle }
    override val seriesCover: Flow<String?> = successState.map { it.series.coverHref ?: initialSeriesData.coverHref }
    override val seriesInfo: Flow<ImmutableCollection<Series.Info>> = successState.map { it.series.infoWithoutGenre }
    override val seriesSeason: Flow<Series.Season?> = successState.map { it.series.currentSeason }
    override val seriesSeasonList: Flow<ImmutableCollection<Series.Season>> = successState.map { it.series.seasons }
    override val seriesLanguage: Flow<Series.Language?> = successState.map { it.series.currentLanguage }
    override val seriesLanguageList: Flow<ImmutableCollection<Series.Language>> = successState.map { it.series.languages }
    override val seriesDescription: Flow<String> = successState.map { it.series.description }
    override val seriesIsAnime: Flow<Boolean> = successState.map { it.series.isAnime }
    override val episodes: Flow<ImmutableCollection<Series.Episode>> = successState.map { it.series.episodes }

    private val episodeStateMachine by instance<EpisodeStateMachine>()
    override val episodeState: StateFlow<EpisodeState> = episodeStateMachine.state.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = EpisodeState.None
    )

    init {
        seriesStateMachine.href(seriesData.toHref())
    }

    @Composable
    override fun render() {
        val haze = remember { HazeState() }

        CompositionLocalProvider(
            LocalHaze provides haze
        ) {
            onRenderWithScheme(initialSeriesData.source) {
                MediumScreen(this, it)
            }
        }
    }

    override fun back() {
        onBack()
    }

    override fun season(value: Series.Season) {
        seriesStateMachine.href(seriesData.toHref(newSeason = value.value))
    }

    override fun language(value: Series.Language) {
        seriesStateMachine.href(seriesData.toHref(newLanguage = value.value))
    }

    override fun episode(value: Series.Episode) {
        launchIO {
            episodeStateMachine.dispatch(EpisodeAction.Load(value))
        }
    }

    override fun watch(value: Series.Episode, streams: ImmutableCollection<Stream>) {
        launchIO {
            episodeStateMachine.dispatch(EpisodeAction.Clear)
            withMainContext {
                onWatch(value, streams)
            }
        }
    }
}
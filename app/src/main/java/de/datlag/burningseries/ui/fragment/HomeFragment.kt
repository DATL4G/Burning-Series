package de.datlag.burningseries.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hadiyarajesh.flower.Resource
import dagger.hilt.android.AndroidEntryPoint
import de.datlag.burningseries.BuildConfig
import de.datlag.burningseries.R
import de.datlag.burningseries.adapter.LatestEpisodeRecyclerAdapter
import de.datlag.burningseries.adapter.LatestSeriesRecyclerAdapter
import de.datlag.burningseries.common.*
import de.datlag.burningseries.databinding.FragmentHomeBinding
import de.datlag.burningseries.extend.AdvancedFragment
import de.datlag.burningseries.viewmodel.*
import de.datlag.model.Constants
import de.datlag.model.burningseries.home.LatestEpisode
import de.datlag.model.burningseries.home.LatestSeries
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@AndroidEntryPoint
@Obfuscate
class HomeFragment : AdvancedFragment(R.layout.fragment_home) {

	private val binding: FragmentHomeBinding by viewBinding()
	private val burningSeriesViewModel: BurningSeriesViewModel by activityViewModels()
	private val settingsViewModel: SettingsViewModel by activityViewModels()
	private val gitHubViewModel: GitHubViewModel by activityViewModels()
	private val userViewModel: UserViewModel by activityViewModels()
	private val usageViewModel: UsageViewModel by activityViewModels()

	private val latestEpisodeRecyclerAdapter by lazy {
		LatestEpisodeRecyclerAdapter(coversDir, blurHash, binding.allSeriesButton.id)
	}
	private val latestSeriesRecyclerAdapter by lazy {
		LatestSeriesRecyclerAdapter(coversDir, blurHash, binding.allSeriesButton.id, extendedFab?.id)
	}

	private var showingHelpImprove: Boolean = false
	private var showingNewVersion: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		hideKeyboard()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recoverMalAuthState()
		recoverAniListAuthState()
		recoverGitHubAuthState()

		initRecycler()
		checkIfErrorCaught()
		listenImproveDialogSetting()
		listenNewVersionDialog()
		listenAllSeriesCount()

		burningSeriesViewModel.homeData.distinctUntilChanged().launchAndCollect {
			when (it.status) {
				Resource.Status.LOADING -> {
					// TODO("show loading indicator")
					it.data?.let { home ->
						latestEpisodeRecyclerAdapter.submitList(home.latestEpisodes)
						latestSeriesRecyclerAdapter.submitList(home.latestSeries)
					}
				}
				Resource.Status.SUCCESS -> {
					latestSeriesRecyclerAdapter.submitList(it.data?.latestSeries ?: listOf())
					latestEpisodeRecyclerAdapter.submitList((it.data?.latestEpisodes ?: listOf()))
					// TODO("hide loading indicator")
				}
				Resource.Status.ERROR -> {
					// TODO("show error snackbar or something")
				}
			}
		}

		binding.allSeriesButton.setOnClickListener {
			findNavController().safeNavigate(HomeFragmentDirections.actionHomeFragmentToAllSeriesFragment())
		}

		extendedFabFavorite(HomeFragmentDirections.actionHomeFragmentToFavoritesFragment())
		extendedFab?.id?.let { binding.allSeriesButton.nextFocusRightId = it }
	}

	private fun showSettingsBadgeWith(text: CharSequence?, success: Boolean) {
	}

	private fun checkIfErrorCaught() {
		val errorText = loadFileSavedText(Constants.LOG_FILE)?.trim()
		if (!errorText.isNullOrEmpty()) {
			showSettingsBadgeWith(safeContext.getString(R.string.exclamation_mark), false)
		}
	}

	private fun listenIsSponsoring() = userViewModel.getGitHubUser().launchAndCollect { user ->
		fun displayDialog(isLoggedIn: Boolean) {
			materialDialogBuilder {
				setPositiveButtonIcon(R.drawable.ic_baseline_celebration_24)
				setNegativeButtonIcon(R.drawable.ic_baseline_close_24)
				if (!isLoggedIn) {
					setNeutralButtonIcon(R.drawable.ic_github)
				}
				builder {
					setTitle(R.string.sponsor_header)
					setMessage(R.string.sponsor_text)
					setPositiveButton(R.string.donate) { dialog, _ ->
						dialog.dismiss()
						Constants.GITHUB_SPONSOR.toUri().openInBrowser(safeContext, safeContext.getString(R.string.sponsor_header))
					}
					setNegativeButton(R.string.close) { dialog, _ ->
						dialog.cancel()
					}
					if (!isLoggedIn) {
						setNeutralButton(R.string.login) { dialog, _ ->
							dialog.dismiss()
							findNavController().safeNavigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
						}
					}
				}
			}.show()
			usageViewModel.showedDonate = true
		}

		if (user != null) {
			combine(userViewModel.getGitHubSponsorStatus(user), userViewModel.getGitHubContributionStatus(user)) { sponsor, contributor ->
				sponsor || contributor
			}.launchAndCollect { isSponsorOrContributor ->
				if (!isSponsorOrContributor) {
					displayDialog(true)
				}
			}
		} else {
			displayDialog(false)
		}
	}

	private fun listenImproveDialogSetting() = settingsViewModel.data.map { it.appearance.improveDialog }.launchAndCollect {
		if (it) {
			if (!burningSeriesViewModel.showedHelpImprove) {
				burningSeriesViewModel.getSeriesCount().launchAndCollect { count ->
					if (count != null) {
						if (!burningSeriesViewModel.showedHelpImprove && !showingNewVersion) {
							materialDialogBuilder {
								setPositiveButtonIcon(R.drawable.ic_baseline_check_24)
								builder {
									setTitle(R.string.help_improve_header)
									setMessage(safeContext.getString(R.string.help_improve_text, count))
									setPositiveButton(R.string.i_understand) { dialog, _ ->
										dialog.dismiss()
									}
									setOnCancelListener {
										showingHelpImprove = false
									}
									setOnDismissListener {
										showingHelpImprove = false
									}
								}
							}.show()
							showingHelpImprove = true
							burningSeriesViewModel.showedHelpImprove = true
						}
					}
				}
			}
		} else {
			burningSeriesViewModel.showedHelpImprove = true
		}
	}

	private fun listenAppUsage() = settingsViewModel.data.map { it.usage.spentTime }.launchAndCollect {
		if (it <= Constants.WEEK_IN_SECONDS && !usageViewModel.showedDonate) {
			if (Math.random() < 0.5) {
				usageViewModel.showedDonate = true
			} else {
				listenIsSponsoring()
			}
		}
	}

	private fun recoverMalAuthState() = settingsViewModel.data.map { it.user.malAuth }.launchAndCollect {
		userViewModel.loadMalAuth(it)
	}

	private fun recoverAniListAuthState() = settingsViewModel.data.map { it.user.anilistAuth }.launchAndCollect {
		userViewModel.loadAniListAuth(it)
	}

	private fun recoverGitHubAuthState() = settingsViewModel.data.map { it.user.githubAuth }.launchAndCollect {
		userViewModel.loadGitHubAuth(it)

		listenAppUsage()
	}

	private fun listenNewVersionDialog() = gitHubViewModel.getLatestRelease().launchAndCollect {
		if (view != null && it != null) {
			showSettingsBadgeWith(safeContext.getString(R.string.arrow_up), true)
		}
		val installedFromFDroid = this@HomeFragment.safeContext.isInstalledFromFDroid()
		if (it != null && !gitHubViewModel.showedNewVersion && !showingHelpImprove) {
			materialDialogBuilder {
				setPositiveButtonIcon(R.drawable.ic_baseline_remove_red_eye_24)
				setNegativeButtonIcon(R.drawable.ic_baseline_close_24)
				builder {
					setTitle(it.title)
					setMessage(safeContext.getString(R.string.new_release_text, "[0-9.]+".toRegex().find(it.tagName)?.value, BuildConfig.VERSION_NAME))
					setPositiveButton(R.string.view) { dialog, _ ->
						dialog.dismiss()
						if (installedFromFDroid) {
							"${Constants.F_DROID_PACKAGES_URL}/${this@HomeFragment.safeContext.packageName}".toUri()
						} else {
							it.htmlUrl.toUri()
						}.openInBrowser(safeContext, safeContext.getString(R.string.new_release))
					}
					setNegativeButton(R.string.close) { dialog, _ ->
						dialog.cancel()
					}
					setOnCancelListener {
						showingNewVersion = false
					}
					setOnDismissListener {
						showingNewVersion = false
					}
				}
			}.show()
			showingNewVersion = true
			gitHubViewModel.showedNewVersion = true
		}
	}

	private fun listenAllSeriesCount() = burningSeriesViewModel.getAllSeriesCountJoined().distinctUntilChanged().launchAndCollect {
		if (it <= 0) {
			burningSeriesViewModel.allSeriesPagination.launchAndCollect {
				burningSeriesViewModel.getNewPaginationData()
			}
		}
	}

	private fun initRecycler(): Unit = with(binding) {
		latestEpisodeRecycler.layoutManager = GridLayoutManager(safeContext, 2, RecyclerView.HORIZONTAL, false)
		latestEpisodeRecycler.adapter = latestEpisodeRecyclerAdapter
		latestEpisodeRecyclerAdapter.setOnClickListener { item ->
			findNavController().safeNavigate(HomeFragmentDirections.actionHomeFragmentToSeriesFragment(
				latestEpisode = item
			))
		}
		
		latestEpisodeRecyclerAdapter.setOnLongClickListener { item ->
			openInBrowser(episode = item)
			true
		}
		

		latestSeriesRecycler.layoutManager = GridLayoutManager(safeContext, 2, RecyclerView.HORIZONTAL, false)
		latestSeriesRecycler.adapter = latestSeriesRecyclerAdapter
		latestSeriesRecyclerAdapter.setOnClickListener { item ->
			findNavController().safeNavigate(HomeFragmentDirections.actionHomeFragmentToSeriesFragment(
				latestSeries = item
			))
		}

		latestSeriesRecyclerAdapter.setOnLongClickListener { item ->
			openInBrowser(series = item)
			true
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		menu.clear()
		inflater.inflate(R.menu.home_menu, menu)

		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.home_github -> {
				Constants.GITHUB_PROJECT.toUri().openInBrowser(safeContext, safeContext.getString(R.string.github))
				true
			}
			R.id.home_settings -> {
				findNavController().safeNavigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
				true
			}
			R.id.home_about -> {
				findNavController().safeNavigate(HomeFragmentDirections.actionHomeFragmentToAboutLibraries())
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onResume() {
		super.onResume()
		burningSeriesViewModel.cancelFetchSeries()
		burningSeriesViewModel.setSeriesData(null)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		Timber.e("Destroyed View in Home")
	}

	private fun openInBrowser(episode: LatestEpisode? = null, series: LatestSeries? = null) {
		val text = safeContext.getString(R.string.open_in_browser_text, (if (episode != null) {
			val (title, episodeTitle) = episode.getEpisodeAndSeries()
			"$episodeTitle\" ${safeContext.getString(R.string.of)} \"$title"
		} else series?.title ?: String()))
		val title = episode?.getEpisodeAndSeries()?.second ?: series?.title
		materialDialogBuilder {
			setPositiveButtonIcon(R.drawable.ic_baseline_arrow_outward_24)
			setNegativeButtonIcon(R.drawable.ic_baseline_close_24)
			builder {
				setTitle(R.string.open_in_browser)
				setMessage(text)
				setPositiveButton(R.string.open) { dialog, _ ->
					dialog.dismiss()
					Constants.getBurningSeriesLink(episode?.href ?: series?.href ?: String()).toUri().openInBrowser(safeContext, title)
				}
				setNegativeButton(R.string.close) { dialog, _ ->
					dialog.cancel()
				}
			}
		}.show()
	}

	override fun initActivityViews() {
		super.initActivityViews()

		exitFullScreen()
		hideSeriesArc()
		extendedFab?.visible()
		hideNavigationFabs()
		setHasOptionsMenu(true)
		hideToolbarBackButton()
		hideSeriesArc()
		appBarLayout?.setExpanded(false, false)
		appBarLayout?.setExpandable(false)
		setToolbarTitle(R.string.app_name)
	}
}
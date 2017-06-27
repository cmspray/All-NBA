package com.gmail.jorgegilcavazos.ballislife.features.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.repository.posts.PostsRepository;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesFragment;
import com.gmail.jorgegilcavazos.ballislife.features.highlights.HighlightsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.login.LoginActivity;
import com.gmail.jorgegilcavazos.ballislife.features.posts.PostsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.profile.ProfileActivity;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.standings.StandingsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.tour.TourLoginActivity;
import com.gmail.jorgegilcavazos.ballislife.util.ActivityUtils;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.SchedulerProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import jonathanfinerty.once.Once;

import static com.gmail.jorgegilcavazos.ballislife.data.RedditAuthentication.REDDIT_AUTH_PREFS;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String SHOW_TOUR = "showTourTag";
    private static final String SUBSCRIBE_TO_TOPICS = "subscribeToTopics";

    private static final String SELECTED_FRAGMENT_KEY = "selectedFragment";
    private static final String SELECTED_SUBREDDIT_KEY = "selectedSubreddit";

    private static final int GAMES_FRAGMENT_ID = 1;
    private static final int STANDINGS_FRAGMENT_ID = 2;
    private static final int POSTS_FRAGMENT_ID = 3;
    private static final int HIGHLIGHTS_FRAGMENT_ID = 4;

    // Shortcuts should match values in xml/shortcuts
    private static final String SHORTCUT_KEY = "shortcut";
    private static final String SHORTCUT_RNBA = "shortcut_rnba";
    private static final String SHORTCUT_HIGHLIGHTS = "shortcut_highlights";

    // Dynamic shortcut
    private static final String TEAM_SUB_SHORTCUT_ID = "teamSubShortcut";
    private static final String SHORTCUT_TEAM_SUB = "shortcut_teamsub";

    // Should match value in strings.xml
    private static final String NO_FAV_TEAM_VAL = "noteam";

    @Inject
    LocalRepository localRepository;

    @Inject
    PostsRepository postsRepository;

    Toolbar toolbar;
    ActionBar actionBar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    int selectedFragment;
    String subreddit;

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BallIsLifeApplication.getAppComponent().inject(this);
        setContentView(R.layout.activity_main);

        // Show app tour if first install.
        if (!Once.beenDone(Once.THIS_APP_INSTALL, SHOW_TOUR)) {
            Intent intent = new Intent(this, TourLoginActivity.class);
            startActivity(intent);
            Once.markDone(SHOW_TOUR);
        }

        // Subscribe to all default firebase messaging topics if first install.
        if (!Once.beenDone(Once.THIS_APP_INSTALL, SUBSCRIBE_TO_TOPICS)) {
            subscribeToDefaultTopics();
        }

        setUpToolbar();
        setUpNavigationView();
        setUpDrawerContent();
        loadRedditUsername();
        setupDynamicShortcut();

        SharedPreferences preferences = getSharedPreferences(REDDIT_AUTH_PREFS, MODE_PRIVATE);
        BaseSchedulerProvider schedulerProvider = SchedulerProvider.getInstance();
        disposables = new CompositeDisposable();
        disposables.add(RedditAuthentication.getInstance().authenticate(preferences)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        loadRedditUsername();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );

        // Set default to GamesFragment.
        selectedFragment = GAMES_FRAGMENT_ID;
        // Default posts fragment subreddit is r/nba
        subreddit = "nba";
        if (savedInstanceState != null) {
            selectedFragment = savedInstanceState.getInt(SELECTED_FRAGMENT_KEY);
            subreddit = savedInstanceState.getString(SELECTED_SUBREDDIT_KEY);
        }

        // Setup opening fragment if app opened from shorcut.
        if (getIntent().getExtras() != null) {
            String shortcut = getIntent().getStringExtra(SHORTCUT_KEY);
            if (shortcut != null) {
                if (shortcut.equals(SHORTCUT_RNBA)) {
                    subreddit = "nba";
                    selectedFragment = POSTS_FRAGMENT_ID;
                } else if (shortcut.equals(SHORTCUT_HIGHLIGHTS)) {
                    selectedFragment = HIGHLIGHTS_FRAGMENT_ID;
                } else if (shortcut.equals(SHORTCUT_TEAM_SUB)) {
                    String teamSub = getTeamSubFromFavoritePref();
                    if (teamSub != null) {
                        setPostsFragment(teamSub);
                        return;
                    }
                }
            }
        }

        switch (selectedFragment) {
            case GAMES_FRAGMENT_ID:
                setGamesFragment();
                break;
            case STANDINGS_FRAGMENT_ID:
                setStandingsFragment();
                break;
            case POSTS_FRAGMENT_ID:
                setPostsFragment(subreddit);
                break;
            case HIGHLIGHTS_FRAGMENT_ID:
                setHighlightsFragment();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_FRAGMENT_KEY, selectedFragment);
        outState.putString(SELECTED_SUBREDDIT_KEY, subreddit);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRedditUsername();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                // Show menu icon
                actionBar.setHomeAsUpIndicator(R.mipmap.ic_menu_white);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void setUpNavigationView() {
        if (toolbar != null) {
            drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            navigationView = (NavigationView) findViewById(R.id.navigation);
            if (navigationView != null) {
                NavigationMenuView navMenuView = (NavigationMenuView) navigationView.getChildAt(0);
                if (navMenuView != null) {
                    navMenuView.setVerticalScrollBarEnabled(false);
                }
            }
        }
    }

    private void setUpDrawerContent() {
        navigationView.setNavigationItemSelectedListener(new NavigationView
                .OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.navigation_item_1:
                        setGamesFragment();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.navigation_item_2:
                        setStandingsFragment();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.navigation_item_3:
                        setPostsFragment("NBA");
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.navigation_item_4:
                        setHighlightsFragment();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.navigation_item_7:
                        // Start LoginActivity if no user is already logged in.
                        if (!RedditAuthentication.getInstance().isUserLoggedIn()) {
                            Intent loginIntent = new Intent(getApplicationContext(),
                                    LoginActivity.class);
                            startActivity(loginIntent);
                        } else {
                            Intent profileIntent = new Intent(getApplicationContext(),
                                    ProfileActivity.class);
                            startActivity(profileIntent);
                        }
                        return true;
                    case R.id.navigation_item_9:
                        drawerLayout.closeDrawer(GravityCompat.START);
                        Intent settingsIntent = new Intent(getApplicationContext(),
                                SettingsActivity.class);
                        startActivity(settingsIntent);
                        return true;
                    case R.id.nav_atl:
                        setPostsFragment(Constants.SUB_ATL);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_bkn:
                        setPostsFragment(Constants.SUB_BKN);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_bos:
                        setPostsFragment(Constants.SUB_BOS);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_cha:
                        setPostsFragment(Constants.SUB_CHA);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_chi:
                        setPostsFragment(Constants.SUB_CHI);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_cle:
                        setPostsFragment(Constants.SUB_CLE);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_dal:
                        setPostsFragment(Constants.SUB_DAL);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_den:
                        setPostsFragment(Constants.SUB_DEN);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_det:
                        setPostsFragment(Constants.SUB_DET);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_gsw:
                        setPostsFragment(Constants.SUB_GSW);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_hou:
                        setPostsFragment(Constants.SUB_HOU);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_ind:
                        setPostsFragment(Constants.SUB_IND);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_lac:
                        setPostsFragment(Constants.SUB_LAC);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_lal:
                        setPostsFragment(Constants.SUB_LAL);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_mem:
                        setPostsFragment(Constants.SUB_MEM);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_mia:
                        setPostsFragment(Constants.SUB_MIA);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_mil:
                        setPostsFragment(Constants.SUB_MIL);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_min:
                        setPostsFragment(Constants.SUB_MIN);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_nop:
                        setPostsFragment(Constants.SUB_NOP);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_nyk:
                        setPostsFragment(Constants.SUB_NYK);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_okc:
                        setPostsFragment(Constants.SUB_OKC);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_orl:
                        setPostsFragment(Constants.SUB_ORL);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_phi:
                        setPostsFragment(Constants.SUB_PHI);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_pho:
                        setPostsFragment(Constants.SUB_PHO);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_por:
                        setPostsFragment(Constants.SUB_POR);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_sac:
                        setPostsFragment(Constants.SUB_SAC);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_sas:
                        setPostsFragment(Constants.SUB_SAS);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_tor:
                        setPostsFragment(Constants.SUB_TOR);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_uta:
                        setPostsFragment(Constants.SUB_UTA);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.nav_was:
                        setPostsFragment(Constants.SUB_WAS);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    default:
                        setGamesFragment();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                }
            }
        });
    }

    public void setGamesFragment() {
        setTitle("Games");
        getSupportActionBar().setSubtitle(null);

        GamesFragment gamesFragment = null;
        if (selectedFragment == GAMES_FRAGMENT_ID) {
            gamesFragment = (GamesFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment);
        }

        if (gamesFragment == null) {
            gamesFragment = GamesFragment.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    gamesFragment, R.id.fragment);
        }

        selectedFragment = GAMES_FRAGMENT_ID;
    }

    public void setStandingsFragment() {
        setTitle("Standings");
        getSupportActionBar().setSubtitle(null);

        StandingsFragment standingsFragment = null;
        if (selectedFragment == STANDINGS_FRAGMENT_ID) {
            standingsFragment = (StandingsFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment);
        }

        if (standingsFragment == null) {
            standingsFragment = StandingsFragment.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    standingsFragment, R.id.fragment);
        }

        selectedFragment = STANDINGS_FRAGMENT_ID;
    }

    public void setPostsFragment(String subreddit) {
        setTitle("r/" + subreddit);
        getSupportActionBar().setSubtitle(null);

        PostsFragment postsFragment = null;
        if (selectedFragment == POSTS_FRAGMENT_ID) {
            postsFragment = (PostsFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment);
        }

        if (postsFragment == null || !this.subreddit.equals(subreddit)) {
            postsRepository.clearCache();
            postsFragment = PostsFragment.newInstance(subreddit);
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    postsFragment, R.id.fragment);
        }

        selectedFragment = POSTS_FRAGMENT_ID;
        this.subreddit = subreddit;
    }

    public void setHighlightsFragment() {
        setTitle("Highlights");
        getSupportActionBar().setSubtitle(null);

        HighlightsFragment highlightsFragment = null;
        if (selectedFragment == HIGHLIGHTS_FRAGMENT_ID) {
            highlightsFragment = (HighlightsFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment);
        }

        if (highlightsFragment == null) {
            highlightsFragment = HighlightsFragment.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    highlightsFragment, R.id.fragment);
        }

        selectedFragment = HIGHLIGHTS_FRAGMENT_ID;
    }

    private void loadRedditUsername() {
        if (navigationView == null) {
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        TextView redditUsername = (TextView) headerView.findViewById(R.id.redditUsername);

        String username = localRepository.getUsername();
        if (username != null) {
            redditUsername.setText(localRepository.getUsername());
        } else {
            redditUsername.setText(R.string.not_logged);
        }
    }

    private void subscribeToDefaultTopics() {
        // Subscribe to all CGA topics
        String[] topics = getResources().getStringArray(R.array.pref_cga_values);
        for (String topic : topics) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic);
        }
    }

    private void setupDynamicShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            String teamSub = getTeamSubFromFavoritePref();
            if (teamSub != null) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, TEAM_SUB_SHORTCUT_ID)
                        .setShortLabel(teamSub)
                        .setLongLabel(teamSub)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_r))
                        .setIntent(new Intent(this, MainActivity.class)
                                .setAction(Intent.ACTION_VIEW)
                                .putExtra(SHORTCUT_KEY, SHORTCUT_TEAM_SUB))
                        .build();

                shortcutManager.setDynamicShortcuts(Arrays.asList(shortcutInfo));
            }
        }
    }

    private String getTeamSubFromFavoritePref() {
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String favTeamVal = defaultPreferences.getString("teams_list", null);
        if (favTeamVal != null && !favTeamVal.equals(NO_FAV_TEAM_VAL)) {
            return RedditUtils.getSubredditFromAbbr(favTeamVal);
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        switch (selectedFragment) {
            case GAMES_FRAGMENT_ID:
                // Exit application.
                super.onBackPressed();
                break;
            default:
                // Return to games fragment.
                setGamesFragment();
                // Expand toolbar in case it's collapsed and empty games recycler doesn't allow
                // scrolling.
                if (toolbar.getParent() instanceof AppBarLayout) {
                    ((AppBarLayout) toolbar.getParent()).setExpanded(true, true);
                }
                navigationView.getMenu().getItem(0).setChecked(true);
                break;
        }
    }
}

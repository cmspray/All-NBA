package com.gmail.jorgegilcavazos.ballislife.data.local;

import android.content.SharedPreferences;

import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishCard;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsFragment;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class AppLocalRepository implements LocalRepository {
    private SharedPreferences localSharedPreferences;
    private SharedPreferences defaultSharedPreferences;

    @Inject
    public AppLocalRepository(
            @Named("localSharedPreferences") SharedPreferences localSharedPreferences,
            @Named("defaultSharedPreferences") SharedPreferences defaultSharedPreferences) {
        this.localSharedPreferences = localSharedPreferences;
        this.defaultSharedPreferences = defaultSharedPreferences;
    }

    @Override
    public void saveFavoritePostsViewType(int viewType) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putInt(LocalSharedPreferences.POSTS_VIEW_TYPE, viewType);
        editor.apply();
    }

    @Override
    public int getFavoritePostsViewType() {
        return localSharedPreferences.getInt(LocalSharedPreferences.POSTS_VIEW_TYPE,
                Constants.POSTS_VIEW_WIDE_CARD);
    }

    @Override
    public void saveFavoriteHighlightViewType(HighlightViewType viewType) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putInt(LocalSharedPreferences.HIGHLIGHTS_VIEW_TYPE, viewType.getValue());
        editor.apply();
    }

    @Override
    public HighlightViewType getFavoriteHighlightViewType() {
        int value = localSharedPreferences.getInt(LocalSharedPreferences.HIGHLIGHTS_VIEW_TYPE,
                                                  HighlightViewType.SMALL.getValue());
        for (HighlightViewType viewType : HighlightViewType.values()) {
            if (viewType.getValue() == value) {
                return viewType;
            }
        }
        saveFavoriteHighlightViewType(HighlightViewType.SMALL);
        return HighlightViewType.SMALL;
    }

    @Override
    public void saveUsername(String username) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putString(LocalSharedPreferences.USERNAME, username);
        editor.apply();
    }

    @Override
    public String getUsername() {
        return localSharedPreferences.getString(LocalSharedPreferences.USERNAME, null);
    }

    @Override
    public String getStartupFragment() {
        return defaultSharedPreferences.getString(SettingsFragment.KEY_STARTUP_FRAGMENT,
                SettingsFragment.STARTUP_FRAGMENT_GAMES);
    }

    @Override
    public boolean getOpenYouTubeInApp() {
        return defaultSharedPreferences.getBoolean(SettingsFragment.KEY_YOUTUBE_IN_APP, true);
    }

    @Override
    public boolean getOpenBoxScoreByDefault() {
        return defaultSharedPreferences.getBoolean(SettingsFragment.KEY_OPEN_BOX_SCORE_DEFAULT, false);
    }

    @Override
    public void saveAppTheme(SwishTheme theme) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putInt(LocalSharedPreferences.SWISH_THEME, theme.getValue());
        editor.apply();
    }

    @Override
    public SwishTheme getAppTheme() {
        int value = localSharedPreferences.getInt(LocalSharedPreferences.SWISH_THEME, -1);

        if (SwishTheme.LIGHT.getValue() == value) {
            return SwishTheme.LIGHT;
        } else if (SwishTheme.DARK.getValue() == value) {
            return SwishTheme.DARK;
        } else {
            return SwishTheme.DARK;
        }
    }

    @Override
    public boolean shouldShowWhatsNew() {
        return localSharedPreferences.getBoolean(LocalSharedPreferences.SHOW_WHATS_NEW, true);
    }

    @Override
    public void setShouldShowWhatsNew(boolean showWhatsNew) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putBoolean(LocalSharedPreferences.SHOW_WHATS_NEW, showWhatsNew);
        editor.apply();
    }

    @Override
    public boolean stickyChipsEnabled() {
        return defaultSharedPreferences.getBoolean(
                SettingsFragment.KEY_CHIPS_FOR_RNBA_ORIGINALS, false);
    }

    @Override
    public boolean isUserWhitelisted() {
        String username = getUsername();
        if (username == null) return false;

        List<String> whitelist = new ArrayList<>();
        whitelist.add("Obi-Wan_Ginobili");
        return whitelist.contains(username);
    }

    @Override
    public boolean noSpoilersModeEnabled() {
        return defaultSharedPreferences.getBoolean(SettingsFragment.KEY_NO_SPOILERS_MODE, false);
    }

    @Override
    public boolean swishCardSeen(SwishCard swishCard) {
        return localSharedPreferences.getBoolean(swishCard.getKey(), false);
    }

    @Override
    public void markSwishCardSeen(SwishCard swishCard) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putBoolean(swishCard.getKey(), true /* seen */);
        editor.apply();
    }

    @Override
    public String getFavoriteTeam() {
        String team =  defaultSharedPreferences.getString("teams_list", null);
        if (team == null || team.equals("noteam")) {
            return null;
        }
        return team;
    }

    @Override
    public void incrementScreenViewed(String key) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        int timesSeen = localSharedPreferences.getInt(key, 0) + 1;
        editor.putInt(key, timesSeen);
        editor.apply();
    }

    @Override
    public boolean shouldShowShortcutCard(SwishCard swishCard) {
        String viewCountKey = "";
        String hasSeenKey = "";
        if(swishCard.getKey().equals(SwishCard.HIGHLIGHT_SHORTCUT.getKey())) {
            viewCountKey = Constants.HIGHLIGHTS_VIEWED_COUNT_KEY;
            hasSeenKey = Constants.HIGHLIGHT_SHORTCUT_SEEN;
        }
        return localSharedPreferences.getInt(viewCountKey, 0) >= 10 && !localSharedPreferences.getBoolean(hasSeenKey, false);
    }

    @Override
    public void markShortcutSeen(String key) {
        SharedPreferences.Editor editor = localSharedPreferences.edit();
        editor.putBoolean(key, true /* seen */);
        editor.apply();
    }
}

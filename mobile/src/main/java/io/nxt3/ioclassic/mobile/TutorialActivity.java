package io.nxt3.ioclassic.mobile;

import android.os.Bundle;
import android.support.annotation.Nullable;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

public class TutorialActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.primary)
                .buttonsColor(R.color.accent)
                .title(getString(R.string.tutorial_version_title))
                .description(getString(R.string.tutorial_version_desc))
                .image(R.drawable.version)
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.primary)
                .buttonsColor(R.color.accent)
                .title(getString(R.string.tutorial_play_store_title))
                .description(getString(R.string.tutorial_play_store_desc))
                .image(R.drawable.playstore)
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.primary)
                .buttonsColor(R.color.accent)
                .title(getString(R.string.tutorial_search_title))
                .description(getString(R.string.tutorial_search_desc))
                .image(R.drawable.search)
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.primary)
                .buttonsColor(R.color.accent)
                .title(getString(R.string.tutorial_install_title))
                .description(getString(R.string.tutorial_install_desc))
                .image(R.drawable.install)
                .build());

        addSlide(new UninstallSlide());
    }
}

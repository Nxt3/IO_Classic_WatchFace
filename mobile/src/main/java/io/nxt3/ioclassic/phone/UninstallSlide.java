package io.nxt3.ioclassic.phone;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import agency.tango.materialintroscreen.SlideFragment;


public class UninstallSlide extends SlideFragment {

    private int mMoveOnCounter = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_uninstall_slide, container, false);

        final Button mUninstallButton = (Button) view.findViewById(R.id.uninstall_button);

        //Prompt user to uninstall the app when the user clicks the "Uninstall" button
        mUninstallButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Uri packageUri = Uri.parse("package:io.nxt3.ioclassic.phone");
                final Intent uninstallIntent
                        = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                startActivity(uninstallIntent);
            }
        });

        return view;
    }

    @Override
    public int backgroundColor() {
        return R.color.primary;
    }

    @Override
    public int buttonsColor() {
        return R.color.accent;
    }

    @Override
    public boolean canMoveFurther() {
        ++mMoveOnCounter;

        /**
         * If the user presses the "Done" button 17 times, just let them exit the app since they
         * obviously don't care about it being uninstalled.
         */
       if (mMoveOnCounter == 17) {
           mMoveOnCounter = 0;
           return true;
       } else {
           return false;
       }
    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        String errorMessage = getString(R.string.tutorial_uninstall_insist);

        if (mMoveOnCounter > 5 && mMoveOnCounter <= 10) {
            errorMessage = getString(R.string.tutorial_easter_egg);
        } else if (mMoveOnCounter > 10 && mMoveOnCounter <= 15) {
            errorMessage = getString(R.string.tutorial_easter_egg_2);
        } else if (mMoveOnCounter > 15) {
            errorMessage = getString(R.string.tutorial_easter_egg_3);
        }

        return errorMessage;
    }
}

package tw.tasker.babysitter.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;

import tw.tasker.babysitter.Config;
import tw.tasker.babysitter.R;
import tw.tasker.babysitter.utils.AccountChecker;

public class ProfileActivity extends BaseActivity implements OnClickListener {
    private SignUpListener mListener = new Listener();
    private Fragment mProfileSitterFragment;
    private Fragment mProfileParentFragment;
    private Fragment mProfileParentEditFragment;
    private Fragment mProfileSitterEditFragment;
    private FragmentTransaction mFragmentTransaction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        mProfileSitterFragment = SitterProfileFragment.newInstance(mListener);
        mProfileSitterEditFragment = SitterProfileEditFragment.newInstance(mListener);

        mProfileParentFragment = ParentProfileFragment.newInstance(mListener);
        mProfileParentEditFragment = ParentProfileEditFragment.newInstance(mListener);

        mFragmentTransaction = getSupportFragmentManager().beginTransaction();

        if (savedInstanceState == null) {
            Fragment fragment = null;
            if (AccountChecker.isSitter()) {
                fragment = mProfileSitterFragment;
            } else {
                fragment = mProfileParentFragment;
            }

            mFragmentTransaction.add(R.id.container, fragment).commit();

        }

//		mEdit = (Button) findViewById(R.id.edit);
//		mEdit.setOnClickListener(this);


    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
//		LogUtils.LOGD("vic", "編輯");
//		Fragment fragment = ProfileParentEditFragment.newInstance();
//		getSupportFragmentManager().beginTransaction()
//		.replace(R.id.container, fragment).addToBackStack(null).commit();
    }

    private final class Listener implements SignUpListener {

        @Override
        public void onSwitchToNextFragment(int type) {

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            switch (type) {
                case Config.PARENT_READ_PAGE:
                    ft.replace(R.id.container, mProfileParentFragment).commit();
                    break;

                case Config.PARENT_EDIT_PAGE:
                    ft.replace(R.id.container, mProfileParentEditFragment).commit();
                    break;

                case Config.SITTER_READ_PAGE:
                    ft.replace(R.id.container, mProfileSitterFragment).commit();
                    break;

                case Config.SITTER_EDIT_PAGE:
                    ft.replace(R.id.container, mProfileSitterEditFragment).commit();
                    break;

                default:
                    break;
            }

        }

    }

}

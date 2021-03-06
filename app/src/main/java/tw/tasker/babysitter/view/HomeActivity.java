package tw.tasker.babysitter.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.parse.ParseAnalytics;

import tw.tasker.babysitter.R;
import tw.tasker.babysitter.UserType;
import tw.tasker.babysitter.utils.AccountChecker;
import tw.tasker.babysitter.utils.ParseHelper;

public class HomeActivity extends BaseActivity {
    public static final int REQUEST_DATA_CHECK = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        if (savedInstanceState == null) {

            Fragment fragment = null;
            UserType userType = AccountChecker.getUserType();
            if (userType == UserType.PARENT) { // 爸媽，抓保母資料
                fragment = new ParentHomeFragment();
            } else if (userType == UserType.SITTER) { // 保母，抓爸媽資料
                fragment = new SitterHomeFragment();
            } else if (userType == UserType.LATER) { // 一般，抓爸媽資料
                fragment = new SitterHomeFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment).commit();
        }
        //getActionBar().setDisplayShowHomeEnabled(false);
        ParseAnalytics.trackAppOpened(getIntent());
        // 後續看要不要放在ActionBar之類的
        // mUserInfo.setText("使用者資訊(" + user.getObjectId() + ")："+
        // user.getUsername() );

        ParseHelper.addUserToInstallation();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}

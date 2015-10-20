package tw.tasker.babysitter.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.parse.ParseException;
import com.parse.SaveCallback;

import de.hdodenhof.circleimageview.CircleImageView;
import tw.tasker.babysitter.Config;
import tw.tasker.babysitter.R;
import tw.tasker.babysitter.model.UserInfo;
import tw.tasker.babysitter.utils.DisplayUtils;
import tw.tasker.babysitter.utils.LogUtils;
import tw.tasker.babysitter.utils.ParseHelper;
import tw.tasker.babysitter.utils.PictureHelper;

public class DataCheckParentEditFragment extends Fragment implements OnClickListener {

    private static SignUpListener mListener;
    private TextView mName;
    private TextView mPhone;
    private TextView mAddress;
    private TextView mKidsAge;
    private TextView mKidsGender;
    private Button mConfirm;
    private CircleImageView mAvatar;
    private PictureHelper mPictureHelper;
    private ProgressDialog mRingProgressDialog;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private Spinner mKidsAgeYear;
    private Spinner mKidsAgeMonth;
    private ScrollView mAllScreen;
    private TextView mJob;
    private TextView mNote;
    private View mRootView;

    public DataCheckParentEditFragment() {
        // Required empty public constructor
    }

    public static Fragment newInstance(SignUpListener listener) {
        Fragment fragment = new DataCheckParentEditFragment();
        mListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_edit_parent_check_data, container, false);

        initView();
        initListener();
        initData();

        return mRootView;
    }

    private void initListener() {
        mAllScreen.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DisplayUtils.hideKeypad(getActivity());
                return false;
            }
        });

        mAvatar.setOnClickListener(this);
        mConfirm.setOnClickListener(this);

    }

    private void initView() {
        mAllScreen = (ScrollView) mRootView.findViewById(R.id.all_screen);
        mAvatar = (CircleImageView) mRootView.findViewById(R.id.avatar);
        mName = (TextView) mRootView.findViewById(R.id.parents_name);
        mPhone = (TextView) mRootView.findViewById(R.id.parents_phone);
        mAddress = (TextView) mRootView.findViewById(R.id.parents_address);
        mJob = (TextView) mRootView.findViewById(R.id.parents_job);
        mKidsAgeMonth = (Spinner) mRootView.findViewById(R.id.kids_age_month);
        //mKidsAge = (TextView) mRootView.findViewById(R.id.kids_age);
        mKidsAgeYear = (Spinner) mRootView.findViewById(R.id.kids_age_year);
        //mKidsGender = (TextView) mRootView.findViewById(R.id.kids_gender);
        mNote = (TextView) mRootView.findViewById(R.id.parents_note);
        mConfirm = (Button) mRootView.findViewById(R.id.confirm);
    }


    protected void initData() {

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.kids_age_year, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKidsAgeYear.setAdapter(adapter);
        mKidsAgeYear.setSelection(DisplayUtils.getPositionFromYear(getActivity()));


        adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.kids_age_month, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKidsAgeMonth.setAdapter(adapter);
        mKidsAgeMonth.setSelection(DisplayUtils.getPositionFromMonth(getActivity()));

    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fillDataToUI(ParseHelper.getParent());

    }


    protected void fillDataToUI(UserInfo userInfo) {
        mName.setText(userInfo.getName());

        mPhone.setText(userInfo.getPhone());
        mAddress.setText(userInfo.getAddress());

        // mKidsAge.setText("小孩生日： 民國 " + year + " 年 " + month + " 月");

        //mKidsGender.setText("小孩姓別：" + userInfo.getKidsGender());

        if (userInfo.getAvatorFile() != null) {
            String url = userInfo.getAvatorFile().getUrl();
            LogUtils.LOGD("vic", "url=" + url);

            imageLoader.displayImage(url, mAvatar, Config.OPTIONS, null);
        } else {
            mAvatar.setImageResource(R.drawable.photo_icon);
        }

    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        switch (id) {
            case R.id.avatar:
                //saveAvatar();
                break;

            case R.id.confirm:
                getActivity().finish();

                //saveUserInfo(Config.userInfo);
                break;
            default:
                break;
        }

    }

    private void saveAvatar() {
        mPictureHelper = new PictureHelper();
        openCamera();
    }

    private void openCamera() {
        Intent intent_camera = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent_camera, 0);
    }

    private void openGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LogUtils.LOGD("vic", "requestCode=" + requestCode + "resultCode=" + resultCode);

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        switch (requestCode) {
            case 0:
                getFromCamera(data);
                break;

            case 1:
                getFromGallery(data);
                break;
            default:
                break;
        }
    }

    private void getFromCamera(Intent data) {
        mRingProgressDialog = ProgressDialog.show(getActivity(),
                "請稍等 ...", "資料儲存中...", true);

        // 取出拍照後回傳資料
        Bundle extras = data.getExtras();
        // 將資料轉換為圖像格式
        Bitmap bmp = (Bitmap) extras.get("data");
        mAvatar.setImageBitmap(bmp);

        mPictureHelper.setBitmap(bmp);
        mPictureHelper.setSaveCallback(new BabyRecordSaveCallback());
        mPictureHelper.savePicture();
    }

    private void getFromGallery(Intent data) {
        mRingProgressDialog = ProgressDialog.show(getActivity(),
                "請稍等 ...", "資料儲存中...", true);

        Uri selectedImage = data.getData();

        String filePath = getFilePath(selectedImage);

        Bitmap bmp = BitmapFactory.decodeFile(filePath);
        mAvatar.setImageBitmap(bmp);

        mPictureHelper.setBitmap(bmp);
        mPictureHelper.setSaveCallback(new BabyRecordSaveCallback());
        mPictureHelper.savePicture();
    }

    private String getFilePath(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    private void saveComment(UserInfo userInfo) {
        //ParseQuery<UserInfo> query = UserInfo.getQuery();
        //query.whereEqualTo("user", ParseUser.getCurrentUser());
        //query.getFirstInBackground(new GetCallback<UserInfo>() {

        //@Override
        //public void done(UserInfo userInfo, ParseException e) {
        userInfo.setAvatorFile(mPictureHelper.getFile());
        userInfo.saveInBackground();
        //}
        //});
        mRingProgressDialog.dismiss();
    }

    private void saveUserInfo(UserInfo userInfo) {
        String phone = mPhone.getText().toString();
        String address = mAddress.getText().toString();

        userInfo.setPhone(phone);
        userInfo.setAddress(address);
        userInfo.setKidsAge(mKidsAgeYear.getSelectedItem().toString() + mKidsAgeMonth.getSelectedItem().toString());
        userInfo.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Toast.makeText(getActivity(),
                            "我的資料更新成功!" /* e.getMessage() */, Toast.LENGTH_LONG)
                            .show();
                    //getActivity().finish();
                    mListener.onSwitchToNextFragment(Config.PARENT_READ_PAGE);
                } else {

                }
            }
        });

    }

    public class BabyRecordSaveCallback extends SaveCallback {

        @Override
        public void done(ParseException e) {
            if (e == null) {
                Toast.makeText(getActivity().getApplicationContext(),
                        "大頭照已上傳..", Toast.LENGTH_SHORT).show();
                saveComment(ParseHelper.getParent());
            } else {
                Toast.makeText(getActivity().getApplicationContext(),
                        "Error saving: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

        }
    }
}

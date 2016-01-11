package tw.tasker.babysitter.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.sdk.messaging.Conversation;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import tw.tasker.babysitter.R;
import tw.tasker.babysitter.UserType;
import tw.tasker.babysitter.adapter.ConversationQueryAdapter;
import tw.tasker.babysitter.adapter.QueryAdapter;
import tw.tasker.babysitter.layer.LayerImpl;
import tw.tasker.babysitter.model.Babysitter;
import tw.tasker.babysitter.model.BabysitterFavorite;
import tw.tasker.babysitter.model.UserInfo;
import tw.tasker.babysitter.parse.ParseImpl;
import tw.tasker.babysitter.utils.AccountChecker;
import tw.tasker.babysitter.utils.DisplayUtils;
import tw.tasker.babysitter.utils.IntentUtil;
import tw.tasker.babysitter.utils.LogUtils;
import tw.tasker.babysitter.utils.ParseHelper;

public class ConversationActivity extends AppCompatActivity implements ConversationQueryAdapter.ConversationClickHandler {
    public static final String PARSE_DATA_KEY = "com.parse.Data";
    private TextView mInfo;
    private Button mOk;
    private UserInfo mUserInfo;
    private ListView mListView;
    //The Query Adapter that grabs all Conversations and displays them based on the last lastMsgContent
    private ConversationQueryAdapter mConversationsAdapter;
    private Dialog mInfoDialog;

    private MenuItem mItem;
    private MenuItem mLogoutItem;
    private MenuItem mProfileItem;
    private SubMenu mSubMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
    }


    @Override
    protected void onResume() {
        super.onResume();

        //If the user is not authenticated, make sure they are logged in, and if they are, re-authenticate
        if (!LayerImpl.isAuthenticated()) {

            if (ParseImpl.getRegisteredUser() == null) {

                LogUtils.LOGD("Activity", "User is not authenticated or logged in - returning to login screen");
                Intent intent = new Intent(ConversationActivity.this, LogInActivity.class);
                startActivity(intent);

            } else {

                LogUtils.LOGD("Activity", "User is not authenticated, but is logged in - re-authenticating user");
                LayerImpl.authenticateUser();

            }

            //Everything is set up, so start populating the Conversation list
        } else {

            LogUtils.LOGD("Activity", "Starting conversation view");
            setupConversationView();
        }

    }

    //Set up the Query Adapter that will drive the RecyclerView on the conversations_screen
    private void setupConversationView() {

        LogUtils.LOGD("Activity", "Setting conversation view");

        //Grab the Recycler View and list all conversation objects in a vertical list
        RecyclerView conversationsView = (RecyclerView) findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        conversationsView.setLayoutManager(layoutManager);

        //The Query Adapter drives the recycler view, and calls back to this activity when the user
        // taps on a Conversation
        mConversationsAdapter = new ConversationQueryAdapter(getApplicationContext(), LayerImpl.getLayerClient(), this, new QueryAdapter.Callback() {
            @Override
            public void onItemInserted() {
                //Log.d("Activity", "Conversation Adapter, new conversation inserted");
            }
        });

        //Attach the Query Adapter to the Recycler View
        conversationsView.setAdapter(mConversationsAdapter);

        //Execute the Query
        mConversationsAdapter.refresh();
    }

    // Conversation
    @Override
    public void onConversationClick(Conversation conversation) {
        //If the Conversation is valid, start the MessageActivity and pass in the Conversation ID
        if (conversation != null && conversation.getId() != null && !conversation.isDeleted()
                && isConfirmBothParentAndSitter(conversation.getId().toString())) {
            Intent intent = new Intent(ConversationActivity.this, MessageActivity.class);
            intent.putExtra("conversation-id", conversation.getId());

            String conversationId = conversation.getId().toString();
            pinParentOrSitterToCache(conversationId);

            startActivity(intent);
        } else if (conversation != null) {
            String conversationId = conversation.getId().toString();

            UserType userType = AccountChecker.getUserType();
            if (userType == UserType.PARENT) { // 爸媽，抓保母資料
                mInfoDialog = DisplayUtils.getSitterDailog(this, conversationId);
            } else {
                mInfoDialog = DisplayUtils.getParentDailog(this, conversationId);
            }

            mInfoDialog.show();

            //DisplayUtils.makeToast(this, "確認「媒合」後，才可以進行對話。");
        }

    }

    private boolean isConfirmBothParentAndSitter(String conversationId) {
        ParseQuery<BabysitterFavorite> query = BabysitterFavorite.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo("conversationId", conversationId);
        query.whereEqualTo("isParentConfirm", true);
        query.whereEqualTo("isSitterConfirm", true);

        BabysitterFavorite favorite = null;
        try {
            favorite = query.getFirst();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (favorite != null) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean onConversationLongClick(Conversation conversation) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onConfirmClick(Conversation conversation) {
        //If the Conversation is valid, start the MessageActivity and pass in the Conversation ID
        if (conversation != null && conversation.getId() != null && !conversation.isDeleted()) {
            Intent intent = new Intent(ConversationActivity.this, MessageActivity.class);
            intent.putExtra("conversation-id", conversation.getId());

            String conversationId = conversation.getId().toString();
            pinParentOrSitterToCache(conversationId);

            startActivity(intent);
        }
    }

    private void pinParentOrSitterToCache(String conversationId) {
        if (AccountChecker.isSitter()) {
            UserInfo parent = ParseHelper.getParentWithConversationId(conversationId);
            ParseHelper.pinParentToCache(parent);
        } else {
            Babysitter sitter = ParseHelper.getSitterWithConversationId(conversationId);
            ParseHelper.pinSitterToCache(sitter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.sign_up, menu);

        mItem = menu.findItem(R.id.action_more);
        mSubMenu = mItem.getSubMenu();
        mLogoutItem = mSubMenu.findItem(R.id.action_logout);
        mProfileItem = mSubMenu.findItem(R.id.action_profile);

        if (ParseUser.getCurrentUser() == null) {
            mLogoutItem.setTitle("登入");
            mProfileItem.setVisible(false);

        } else {
            mLogoutItem.setTitle("登出");
        }

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;

            case R.id.action_profile:
                startActivity(IntentUtil.startProfileActivity());
                break;

            case R.id.action_fb:
                startActivity(IntentUtil.startFacebook());
                break;

            case R.id.action_gmail:
                startActivity(IntentUtil.startEmail());
                break;

            case R.id.action_logout:
                if (ParseUser.getCurrentUser() == null) { // 沒有登入
                } else { // 有登入
                    // Call the Parse log out method
                    ParseUser.logOut();
                }

                startActivity(IntentUtil.startDispatchActivity());

                break;
            default:
                break;
        }


        return super.onOptionsItemSelected(item);
    }

}

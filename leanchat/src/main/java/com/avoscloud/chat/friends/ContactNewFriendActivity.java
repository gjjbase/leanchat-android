package com.avoscloud.chat.friends;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.SaveCallback;
import com.avoscloud.chat.R;
import com.avoscloud.chat.activity.BaseActivity;
import com.avoscloud.chat.friends.AddRequest;
import com.avoscloud.chat.friends.AddRequestManager;
import com.avoscloud.chat.service.ConversationManager;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.chat.event.ContactRefreshEvent;
import com.avoscloud.chat.adapter.BaseListAdapter;
import com.avoscloud.chat.view.BaseListView;
import com.avoscloud.chat.util.RefreshTask;
import com.avoscloud.chat.util.Refreshable;
import com.avoscloud.leanchatlib.model.LeanchatUser;
import com.avoscloud.leanchatlib.utils.PhotoUtils;
import com.avoscloud.leanchatlib.view.ViewHolder;
import com.nostra13.universalimageloader.core.ImageLoader;

import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 新的好友
 */
public class ContactNewFriendActivity extends BaseActivity implements
    Refreshable {
  @InjectView(R.id.newfriendList)
  BaseListView<AddRequest> listView;
  private NewFriendListAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    setContentView(R.layout.contact_new_friend_activity);
    ButterKnife.inject(this);
    initView();
    refresh();
  }

  public void refresh() {
    listView.onRefresh();
  }

  private void initView() {
    initActionBar(R.string.contact_new_friends);
    adapter = new NewFriendListAdapter(this);
    adapter.setListener(new NewFriendListAdapter.Listener() {
      @Override
      public void onAgreeAddRequest(final AddRequest addRequest) {
        final ProgressDialog dialog = showSpinnerDialog();
        AddRequestManager.getInstance().agreeAddRequest(addRequest, new SaveCallback() {
          @Override
          public void done(AVException e) {
            dialog.dismiss();
            if (filterException(e)) {
              if (addRequest.getFromUser() != null) {
                ConversationManager.getInstance().sendWelcomeMessage(addRequest.getFromUser().getObjectId());
              }
              refresh();
              ContactRefreshEvent event = new ContactRefreshEvent();
              EventBus.getDefault().post(event);
            }
          }
        });
      }
    });

    listView.init(new BaseListView.DataFactory<AddRequest>() {
      @Override
      public List<AddRequest> getDatasInBackground(int skip, int limit, List<AddRequest> currentDatas) throws Exception {
        List<AddRequest> addRequests = AddRequestManager.getInstance().findAddRequests(skip, limit);
        AddRequestManager.getInstance().markAddRequestsRead(addRequests);
        List<AddRequest> filters = new ArrayList<AddRequest>();
        for (AddRequest addRequest : addRequests) {
          if (addRequest.getFromUser() != null) {
            filters.add(addRequest);
          }
        }
        addRequests = filters;
        PreferenceMap preferenceMap = new PreferenceMap(ctx, LeanchatUser.getCurrentUserId());
        preferenceMap.setAddRequestN(addRequests.size());
        return addRequests;
      }
    }, adapter);

    listView.setItemListener(new BaseListView.ItemListener<AddRequest>() {
      @Override
      public void onItemLongPressed(final AddRequest item) {
        new AlertDialog.Builder(ctx).setMessage(R.string.contact_deleteFriendRequest)
            .setPositiveButton(R.string.common_sure, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                deleteAddRequest(item);
              }
            }).setNegativeButton(R.string.chat_common_cancel, null).show();
      }
    });
  }

  private void deleteAddRequest(final AddRequest addRequest) {
    new RefreshTask(ctx, this) {
      @Override
      protected void doInBack() throws Exception {
        addRequest.delete();
        refresh();
      }
    }.execute();
  }

  public static class NewFriendListAdapter extends BaseListAdapter<AddRequest> {
    private Listener listener;

    public NewFriendListAdapter(Context ctx) {
      super(ctx);
    }

    public NewFriendListAdapter(Context context, List<AddRequest> list) {
      super(context, list);
    }

    public void setListener(Listener listener) {
      this.listener = listener;
    }

    @Override
    public View getView(int position, View conView, ViewGroup parent) {
      // TODO Auto-generated method stub
      if (conView == null) {
        conView = inflater.inflate(R.layout.contact_add_friend_item, null);
      }
      final AddRequest addRequest = datas.get(position);
      TextView nameView = ViewHolder.findViewById(conView, R.id.name);
      ImageView avatarView = ViewHolder.findViewById(conView, R.id.avatar);
      final Button addBtn = ViewHolder.findViewById(conView, R.id.add);
      View agreedView = ViewHolder.findViewById(conView, R.id.agreedView);

      LeanchatUser from = (LeanchatUser)addRequest.getFromUser();
      ImageLoader.getInstance().displayImage(from.getAvatarUrl(), avatarView, PhotoUtils.avatarImageOptions);
      if (from != null) {
        nameView.setText(from.getUsername());
      }
      int status = addRequest.getStatus();
      if (status == AddRequest.STATUS_WAIT) {
        addBtn.setVisibility(View.VISIBLE);
        agreedView.setVisibility(View.GONE);
        addBtn.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View arg0) {
            // TODO Auto-generated method stub
            if (listener != null) {
              listener.onAgreeAddRequest(addRequest);
            }
          }
        });
      } else if (status == AddRequest.STATUS_DONE) {
        addBtn.setVisibility(View.GONE);
        agreedView.setVisibility(View.VISIBLE);
      }
      return conView;
    }

    public interface Listener {
      void onAgreeAddRequest(AddRequest addRequest);
    }
  }
}

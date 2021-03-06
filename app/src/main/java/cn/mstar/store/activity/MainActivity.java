package cn.mstar.store.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.List;

import cn.mstar.store.R;
import cn.mstar.store.app.MyAction;
import cn.mstar.store.app.MyApplication;
import cn.mstar.store.customviews.LoadingDialog;
import cn.mstar.store.entity.ShoppingCartItem;
import cn.mstar.store.fragments.ClassifyFragment;
import cn.mstar.store.fragments.HomeFragment;
import cn.mstar.store.fragments.MySelfFragment;
import cn.mstar.store.fragments.ShoppingCartFragment;
import cn.mstar.store.utils.ApplicationUpdateThread;
import cn.mstar.store.utils.Constants;
import cn.mstar.store.utils.CustomToast;
import cn.mstar.store.utils.L;
import cn.mstar.store.utils.NewLink;
import cn.mstar.store.utils.Utils;
import cn.mstar.store.utils.VolleyRequest;


public class MainActivity extends AppCompatActivity implements MySelfFragment.OnFragmentInteractionListener, View.OnClickListener {


	private static final String MYSELFFRAGMENT_TAG =  "myselffragmenttag";
	private FragmentManager fragmentManager;
	// Fragment资源
	private Fragment homeFragment, classficationFragment,myselfFragment, shoppingCartItem;
	// 主页按钮编号 有几个写几个
	public static final int TAB_HOME = 0;// 首页
	public static final int TAB_ME = 2;// 我
	public static final int TAB_SHOPPING_CART = 3, TAB_CLASSIFICATION = 4;// 更多
	private RadioGroup mRadioGroup;
	private MyApplication app;
	private String token;
	private int previousId;
	private int nowId;
	private boolean justLogOf =false;
	private AlertDialog alertDialog;

	/**
	 * - each time the program is restarted, always relogin and
	 * 		reupdate the actual version of the key we have.
	 *
	 * - when the activity gets out accidentally or anyway, always,
	 * 		get of the stuff.
	 *
	 * - if the informations are right on the creation of the activity,
	 * 		we keep a true inside the main activity and we directly
	 * 		read the updated information from the SharedPreferences.
	 *
	 * - when we get out, the shared preference is clean, but only
	 * 		the username and the password are kept.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Utils.LoginClean((MyApplication) getApplication(), false);
		Utils.setNavigationBarColor(this, getResources().getColor(R.color.status_bar_color));
		Utils.setStatusBarColor(this, getResources().getColor(R.color.status_bar_color));
		// 设置为竖屏
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		MyApplication.getInstance().addActivity(this);
		init();
		previousId = R.id.radio_home;
		app = (MyApplication) getApplication();
		(new ApplicationUpdateThread(app)).start();
	}

	// each time we onresume... check if there is an update...


	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String orderId = intent.getStringExtra("data");
		// jump to intent
		if (!intent.getAction().equals(MyAction.paySuccessGotodetails))
			return;
		final String link = NewLink.GOPAY_FOR_ORDER + "&key=" + app.tokenKey + "&OrderNum=" + orderId;
		i_showProgressDialog();
		VolleyRequest.GetCookieRequest(this, link, new VolleyRequest.HttpStringRequsetCallBack() {
			@Override
			public void onSuccess(String result) {

				if (!"".equals(result)) {
					Intent intent = new Intent(MainActivity.this, IndentDetailsActivity.class);
					intent.setAction(MyAction.goPayAction);
					// the actions have to get down before we jump to the next.
					intent.putExtra("data", result);
					intent.putExtra("link", link);
					i_dismissProgressDialog();
					startActivity(intent);
				}
			}

			@Override
			public void onFail(String error) {
				i_dismissProgressDialog();
			}
		});
	}

	private void init() {
		// TODO Auto-generated method stub
		token=MyApplication.getInstance().tokenKey;
		fragmentManager = getSupportFragmentManager();
		//默认选中HomeFragment
		mRadioGroup=(RadioGroup) findViewById(R.id.radiogroup);
		previousId = mRadioGroup.getChildAt(0).getId();
		nowId = mRadioGroup.getChildAt(0).getId();
		selectItem(TAB_HOME);
		/*mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				previous = nowCheckedId;
				nowCheckedId = checkedId;
				switch (checkedId) {
					case R.id.radio_home:
						selectItem(TAB_HOME);
						break;
					case R.id.radio_me:
						selectItem(TAB_ME);
						break;
					case R.id.radio_shopping_cart:
						selectItem(TAB_SHOPPING_CART);
						break;
					case R.id.radio_classification:
						selectItem(TAB_CLASSIFICATION);
						break;
				}
			}
		});*/
		for (int i = 0; i < 4; i++) {
			mRadioGroup.getChildAt(i).setOnClickListener(this);
		}

	}


	// 显示选定片段视图导航抽屉列表项
	public void selectItem(int position) {

		L.d("request", "没登录" + position + "");
		justLogOf = false;
		if (justLogOf &&app != null  && "".equals(Utils.getTokenKey(app)) &&
				(position == TAB_ME || position == TAB_SHOPPING_CART)) {
			selectItem(TAB_HOME);
			justLogOf = false;
		} else {

			if (position == TAB_HOME) {
				// 替换首页
				changeHome();
				makeToast("Home");
			} else if (position == TAB_CLASSIFICATION) {
				changeClassification();
				makeToast("Classification");
			} else {
				// 先判断登录了没、 再决定替换
				if ("".equals(Utils.getTokenKey(app))) {
					// 选择目前那个
					checkPrevious();
					// 判断是哪一个fragment再替换
					loginToExchange(position);
					makeToast("没登录");
				} else {
					// 调到登录界面，并监听回来
					makeToast("已登录、jump to " + position);
					switch (position) {
						case TAB_SHOPPING_CART:
							changeShoppingCart();
							break;
						case TAB_ME:
							changeMyself();
							break;
					}
				}
			}
		}
	}


	private void checkPrevious() {
		for (int i = 0; i < 4; i++) {
			if (mRadioGroup.getChildAt(i).getId() == nowId) {
				//
				mRadioGroup.check(nowId);
				break;
			}
		}

	}

	private void loginToExchange(int position) {
		/*switch (position) {
			case TAB_SHOPPING_CART:
				break;
			case TAB_CLASSIFICATION:
				break;
		}*/
		Intent loginIntent = new Intent(this, LoginActivity.class);
		loginIntent.setAction(MyAction.logForExchange);
		loginIntent.putExtra("fragmentid", position);
		startActivityForResult(loginIntent, 94);
	}

	private void changeHome() {
		FragmentTransaction transaction = fragmentManager.beginTransaction();
//		transaction.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
		hideFragments(transaction);
		if (homeFragment == null) {
			homeFragment = new HomeFragment();
			transaction.add(R.id.content_frame, homeFragment);
		} else
			transaction.show(homeFragment);
		transaction.commit();
		((RadioButton)mRadioGroup.getChildAt(0)).setChecked(true);
	}

	private void changeClassification() {
		FragmentTransaction transaction = fragmentManager.beginTransaction();
//		transaction.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
		hideFragments(transaction);
		if (classficationFragment == null) {
			classficationFragment = new ClassifyFragment();
			transaction.add(R.id.content_frame, classficationFragment);
		} else
			transaction.show(classficationFragment);
		transaction.commit();
		((RadioButton)mRadioGroup.getChildAt(1)).setChecked(true);
	}


	private void changeShoppingCart() {
		FragmentTransaction transaction = fragmentManager.beginTransaction();
//		transaction.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
		hideFragments(transaction);
		if (shoppingCartItem == null) {
			shoppingCartItem = new ShoppingCartFragment();
			transaction.add(R.id.content_frame, shoppingCartItem);
		} else {
			if (app.frg_isFrg_shoppingcart_needUpdate == true) {
				((ShoppingCartFragment) shoppingCartItem).inflateDatas();
				app.frg_isFrg_shoppingcart_needUpdate = false;
			}
			transaction.show(shoppingCartItem);
		}
		transaction.commit();
		((RadioButton)mRadioGroup.getChildAt(2)).setChecked(true);
	}

	private void changeMyself() {
		FragmentTransaction transaction = fragmentManager.beginTransaction();
//		transaction.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
		hideFragments(transaction);
		if (myselfFragment == null) {
			myselfFragment = new MySelfFragment();
			transaction.add(R.id.content_frame, myselfFragment);
		} else
		if (app.isFrg_me_needUpdate == true) {
			((MySelfFragment) myselfFragment).updateNumbers();
			app.isFrg_me_needUpdate = false;
		}
		transaction.show(myselfFragment);
		transaction.commit();
		((RadioButton) mRadioGroup.getChildAt(3)).setChecked(true);
	}


	public void loginToServer() {
		Intent loginIntent = new Intent(this, LoginActivity.class);
		loginIntent.setAction(MyAction.logForShoppingCart);
		startActivityForResult(loginIntent, 11);
	}

	private void invokeShoppingCart() {

		i_showProgressDialog();
		VolleyRequest.checkLogStatus((MyApplication) getApplication(), new VolleyRequest.LogonStatusLinstener() {
			@Override
			public void OK(String reason) {

				// 登录完成后
				afterLogin();
				i_dismissProgressDialog();
			}

			@Override
			public void NO() {

				// 未登录
				beforeLogin();
				i_dismissProgressDialog();
			}
		});
	}

	// we create the fragment only once.
	private void afterLogin() {
//		inflateData(link);
		shoppingCartItem = new ShoppingCartFragment();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
//		transaction.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
		hideFragments(transaction);
		transaction.add(R.id.content_frame, shoppingCartItem);
		transaction.show(shoppingCartItem);
		transaction.commit();
		CustomToast.mSystemToast(this, "creating shoppingcartitemfragmnet");
	}

	private void beforeLogin() {

		// 让用户登录
		Intent intent = new Intent(this, LoginActivity.class);
		// give him an action
		intent.setAction(MyAction.logForShoppingCart);
		startActivityForResult(intent, 11);
	}



	LoadingDialog dialog;
	public void i_showProgressDialog() {
		dialog = new LoadingDialog(this);
		dialog.show();
	}

	public void i_dismissProgressDialog () {
		if (dialog != null) {
			dialog.cancel();
			dialog.dismiss();
			dialog = null;
		}
	}

	public void i_showProgressDialog(String message) {
		dialog = new LoadingDialog(this, message);
		dialog.show();
	}




	private void hideFragments(FragmentTransaction transaction) {
		// TODO Auto-generated method stub
		if(homeFragment!=null)
			transaction.hide(homeFragment);
		if(classficationFragment!=null)
			transaction.hide(classficationFragment);
		if(myselfFragment!=null)
			transaction.hide(myselfFragment);
		if(shoppingCartItem!=null)
			transaction.hide(shoppingCartItem);
	}


	private long exitTime=0;
	private boolean myselfFragJustChanged = false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN){
			if(System.currentTimeMillis()-exitTime>2000){
				CustomToast.makeToast(this, getString(R.string.exit_app), Toast.LENGTH_SHORT);
				exitTime=System.currentTimeMillis();
			}else{
//				MyApplication.getInstance().exit();
				moveTaskToBack(true);
			}
		}
		return true;
	}

	protected void replaceFragment(int viewId, Fragment fragment)
	{
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(viewId, fragment)
				.commitAllowingStateLoss();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// if you are not logged then, back to the home
		L.d("request:::", "" + requestCode + " and data is " + data + " and previous is " + nowId);
		if (requestCode == 94) {
			// check the result... and do the supposed action
/*ntent.putExtra("fragmentid", getIntent().getIntExtra("fragmentid", -1));*/
			int fragment_id = -1;
			if (data != null)
				fragment_id = data.getIntExtra("fragmentid", -1);
			else {
				fragment_id = nowId;
				justLogOf = true;
			}
			L.d("request:::---", ""+fragment_id);
//			if (fragment_id != -1) {
			if ("".equals(Utils.getTokenKey(app))) {
				if (fragment_id != TAB_HOME && fragment_id != TAB_CLASSIFICATION)
					selectItem(TAB_HOME);
				else
					selectItem(fragment_id);
			} else {
				selectItem(fragment_id);
			}
//			}
		} else if (requestCode != 94 && data != null) {
// if the result is 11... move back to the previous button.
			if (requestCode == 11 && data != null) {
				// tell the main activity to proceed the change.
				// choose back the previous button
//			selectPrevious();
//			selectItem(TAB_SHOPPING_CART);
			} else if (requestCode == 2 && data != null) {
				app.username = data.getStringExtra(Constants.START_ACTIVITY_FOR_RESULT_KEY);
				app.points = data.getStringExtra(Constants.POINTS);
				app.pic = data.getStringExtra(Constants.PIC);
				makeToast("MAIN points " + app.points);
			}

			// if the code is 94 ... then it doesn't concern the other viewz.

			List<Fragment> fragments = getSupportFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment != null)
						fragment.onActivityResult(requestCode, resultCode, data);
				}
			}
		}
	}

	private void makeToast(String s) {
//		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onFragmentInteraction(int i) {
		if (i == 0) {
			// update fragment .
			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			Fragment df; // = getSupportFragmentManager().findFragmentByTag(MYSELFFRAGMENT_TAG);
			df = MySelfFragment.newInstance(false);
			fragmentTransaction.replace(R.id.content_frame, df, MYSELFFRAGMENT_TAG).commit();
		}
	}

	public void updateBottomTotal(List<ShoppingCartItem> checkedGoods) {
		// call the same function inside the fragment.
		if (shoppingCartItem != null)
			((ShoppingCartFragment)shoppingCartItem).updateBottom(checkedGoods);
	}

/*	public void updateMyselfFrag(boolean isLogged, boolean doswitch) {
//		CustomToast.mToast(this, "updating myselffragment");
		myselfFragJustChanged  = true;
		// the current showing function is me ~ then update it automatically...
		if (myselfFragment != null  && myselfFragment.isVisible() *//*&&
				(myselfFragment == null ? true : !(((MySelfFragment)myselfFragment).getLoggedState() == isLogged))*//*) {

			// update the fragment and show it ~
			myselfFragJustChanged  = true;
			if (doswitch)
				fakeSelectItem(isLogged);
			// i need to know what was the previous state.
		}
	}*/

	private void fakeSelectItem(boolean isLogged) {

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (myselfFragment != null) {
			transaction.hide(myselfFragment);
		}
		if (myselfFragJustChanged) {
			if (myselfFragment != null)
				transaction.remove (myselfFragment);
			myselfFragment= MySelfFragment.newInstance(isLogged);
			transaction.add(R.id.content_frame, myselfFragment, /*my own adding*/ MYSELFFRAGMENT_TAG);
			myselfFragJustChanged = false;
		} else {
			if (myselfFragment == null) {
				myselfFragment= MySelfFragment.newInstance(false);
				transaction.add(R.id.content_frame, myselfFragment, /*my own adding*/ MYSELFFRAGMENT_TAG);
			} else
				transaction.show(myselfFragment);
		}
		transaction.commit();
	}


	@Override
	protected void onResume() {
		super.onResume();
		checkUgradeVersion ();
		if (app == null || (app != null && "".equals(app.tokenKey))) {
			myselfFragment = null;
			return;
		}
		if (app != null)
			app.isFrg_me_needUpdate = true;

		// check in another thread if the fragment myself needs to be updated.

	/*	if (myselfFragment != null && !((MySelfFragment) myselfFragment).getLoggedState ()) {
			// check if the fragment needs to be updated.

			VolleyRequest.checkLogStatus(this, new LogonStatusLinstener() {
				@Override
				public void OK(String reason) {
//					CustomToast.mSystemToast(MainActivity.this, "main logged");
					updateMyselfFrag(true, true);
				}

				@Override
				public void NO() {
					CustomToast.mSystemToast(MainActivity.this, "main not logged");
					updateMyselfFrag(false, true);
				}
			});
		}*/
	}

	private void checkUgradeVersion() {

		// 用户可以随便更新
		if (app != null && app.needUpgrade) {
			// make the user upgrade to something else...
			alertDialog = new AlertDialog.Builder(this).setMessage(getString(R.string.needtoupdate_toversion)/*+ app.upgradeEntity.version*/)
					.setPositiveButton(getString(R.string.upgrade), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// jump to browser...
							jumpToBrowser (app.upgradeEntity.download);
							alertDialog.dismiss();
						}
					}).setNegativeButton(getString(R.string.not_now), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							app.needUpgrade = false;
							alertDialog.dismiss();
						}
					}).setTitle(getString(R.string.upgrade)).create();
			alertDialog.show();
		}
	}

	private void jumpToBrowser(String download) {

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(download));
		startActivity(i);
		app.needUpgrade = false;
	}


	public void isLogged (View view) {

		VolleyRequest.checkLogStatus(app, new VolleyRequest.LogonStatusLinstener() {

			/* 已登录了 */
			@Override
			public void OK(String reason) {
				CustomToast.mSystemToast(MainActivity.this, "logged");
			}

			/* 未登录 */
			@Override
			public void NO() {
				CustomToast.mSystemToast(MainActivity.this, "not logged");
			}
		});

	}

	public int getScreenWidth() {

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.widthPixels;
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		overridePendingTransition(R.anim.custom_in_anim, R.anim.custom_out_anim);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(intent, requestCode);
		overridePendingTransition(R.anim.custom_in_anim, R.anim.custom_out_anim);
	}


	public void mySelfFragmentNeedUpdate(boolean b) {
		myselfFragJustChanged = true;
		i_dismissProgressDialog();
	}


	@Override
	public void onClick(View v) {


		switch (v.getId()) {
			case R.id.radio_home:
				if (!"".equals(Utils.getTokenKey(app)) || v.getId() == R.id.radio_home || v.getId() == R.id.radio_classification) {
//					if (nowId != 0)
					previousId = nowId;
					nowId = TAB_HOME;
				}
				selectItem(TAB_HOME);
				break;
			case R.id.radio_me:
				selectItem(TAB_ME);
				break;
			case R.id.radio_shopping_cart:
				selectItem(TAB_SHOPPING_CART);
				break;
			case R.id.radio_classification:
				if (!"".equals(Utils.getTokenKey(app)) || v.getId() == R.id.radio_home || v.getId() == R.id.radio_classification) {
//					if (nowId != 0)
					previousId = nowId;
					nowId = TAB_CLASSIFICATION;
				}
				selectItem(TAB_CLASSIFICATION);
				break;
		}
	}
}


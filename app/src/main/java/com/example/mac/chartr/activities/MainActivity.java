package com.example.mac.chartr.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.example.mac.chartr.ApiClient;
import com.example.mac.chartr.ApiInterface;
import com.example.mac.chartr.CommonDependencyProvider;
import com.example.mac.chartr.R;
import com.example.mac.chartr.fragments.NearbyFragment;
import com.example.mac.chartr.fragments.ProfileFragment;
import com.example.mac.chartr.fragments.RequestsFragment;
import com.example.mac.chartr.fragments.trips.TripsFragment;
import com.example.mac.chartr.objects.User;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    Toolbar toolbar;
    private String username;
    private CognitoUser user;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;

    private CommonDependencyProvider provider;
    GenericHandler trustedDeviceHandler = new GenericHandler() {
        @Override
        public void onSuccess() {
            // Close wait dialog
            closeWaitDialog();
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            showDialogMessage("Failed to update device status",
                    provider.getAppHelper().formatException(exception), true);
        }
    };
    GetDetailsHandler detailsHandler = new GetDetailsHandler() {
        @Override
        public void onSuccess(CognitoUserDetails cognitoUserDetails) {
            closeWaitDialog();
            // Store details in the AppHandler
            provider.getAppHelper().setUserDetails(cognitoUserDetails);
            // Trusted devices?
            handleTrustedDevice();
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            showDialogMessage("Could not fetch user details!",
                    provider.getAppHelper().formatException(exception), true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCommonDependencyProvider(new CommonDependencyProvider());
        setContentView(R.layout.activity_main);

        if (provider.getAppHelper() == null) {
            provider.getAppHelper(this);
        }

        if (findViewById(R.id.content) != null) {
            if (savedInstanceState != null) {
                return;
            }
            TripsFragment initialFragment = new TripsFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content, initialFragment).commit();

        }

        setupTopToolbar();
        setupBottomNavigation();
        setupToolbarListener();

        // Get the user name
        Bundle extras = getIntent().getExtras();
        username = provider.getAppHelper().getCurrUser();
        user = provider.getAppHelper().getPool().getUser(username);
        getDetails();
    }

    private void setupTopToolbar() {
        final Context context = this;
        toolbar = (Toolbar) findViewById(R.id.topToolBar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));
        Button goToCreateTrip = findViewById(R.id.buttonAddTrip);
        goToCreateTrip.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostTripActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Listener for changing the toolbar title and go to add trip button.
     */
    private void setupToolbarListener() {
        final Activity activity = this;
        getSupportFragmentManager().addOnBackStackChangedListener(
                () -> {
                    int topOfBackstack = getSupportFragmentManager()
                            .getBackStackEntryCount() - 1;

                    // Set top toolbar title
                    String title = "Trips";
                    if (topOfBackstack >= 0) {
                        title = getSupportFragmentManager()
                                .getBackStackEntryAt(topOfBackstack).getName();

                    }
                    getSupportActionBar().setTitle(title);

                    // Show or hide plus button
                    if (title == "Trips") {
                        findViewById(R.id.buttonAddTrip).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.buttonAddTrip).setVisibility(View.GONE);
                    }
                });
    }

    public void setCommonDependencyProvider(CommonDependencyProvider provider) {
        this.provider = provider;
    }

    public void signOut() {
        user.signOut();
        exit();
    }

    /**
     * Get user details from CIP service and from the API Gateway.
     */
    private void getDetails() {
        provider.getAppHelper().getPool().getUser(username).getDetailsInBackground(detailsHandler);

        ApiInterface apiInterface = ApiClient.getApiInstance();
        callGetUserApi(apiInterface, username);
    }

    /**
     * Calls api to get a user with given username.
     *
     * @param apiInterface Contains api calls
     * @param username the username (email) of the user
     */
    private void callGetUserApi(ApiInterface apiInterface, String username) {
        Call<User> call;
        call = apiInterface.getUser(username);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                int code = response.code();
                if (code == 200) {
                    Log.d(TAG, "User gotten successfully.");
                    provider.getAppHelper().setLoggedInUser(response.body());
                } else {
                    Log.d(TAG, "Retrofit failed to get user, response code: "
                            + response.code());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.d(TAG, "Retrofit failed to get user.");
                Log.e(TAG, t.getMessage());
                t.printStackTrace();
                call.cancel();
            }
        });
    }

    private void handleTrustedDevice() {
        CognitoDevice newDevice = provider.getAppHelper().getNewDevice();
        if (newDevice != null) {
            provider.getAppHelper().newDevice(null);
            trustedDeviceDialog(newDevice);
        }
    }

    private void trustedDeviceDialog(final CognitoDevice newDevice) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remember this device?");
        //final EditText input = new EditText(UserActivity.this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        //input.setLayoutParams(lp);
        //input.requestFocus();
        //builder.setView(input);

        builder.setPositiveButton("Yes", (dialog, which) -> {
            try {
                //String newValue = input.getText().toString();
                showWaitDialog("Remembering this device...");
                updateDeviceStatus(newDevice);
                userDialog.dismiss();
            } catch (Exception e) {
                // Log failure
            }
        }).setNegativeButton("No", (dialog, which) -> {
            try {
                userDialog.dismiss();
            } catch (Exception e) {
                // Log failure
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void updateDeviceStatus(CognitoDevice device) {
        device.rememberThisDeviceInBackground(trustedDeviceHandler);
    }

    private void setupBottomNavigation() {
        Log.d(TAG, "Setting up Bottom Navigation");
        final BottomNavigationViewEx navBar = findViewById(R.id.bottomNavigationBar);
        final Context context = this;
        navBar.enableAnimation(false);
        navBar.enableShiftingMode(false);
        navBar.enableItemShiftingMode(false);
        navBar.setTextVisibility(true);
        navBar.setSelectedItemId(R.id.ic_trips);
        navBar.setOnNavigationItemSelectedListener(
                item -> {
                    int itemId = item.getItemId();
                    switch (itemId) {
                        case R.id.ic_nearby:
                            getSupportActionBar().setTitle("Nearby");
                            findViewById(R.id.buttonAddTrip).setVisibility(View.GONE);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.content, new NearbyFragment())
                                    .addToBackStack("Nearby").commit();
                            break;
                        case R.id.ic_trips:
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.content, new TripsFragment())
                                    .addToBackStack("Trips").commit();
                            getSupportActionBar().setTitle("Trips");
                            findViewById(R.id.buttonAddTrip).setVisibility(View.VISIBLE);

                            break;
                        case R.id.ic_requests:
                            getSupportActionBar().setTitle("Requests");
                            findViewById(R.id.buttonAddTrip).setVisibility(View.GONE);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.content, new RequestsFragment())
                                    .addToBackStack("Requests").commit();
                            break;
                        case R.id.ic_profile:
                            getSupportActionBar().setTitle("Profile");
                            findViewById(R.id.buttonAddTrip).setVisibility(View.GONE);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.content, new ProfileFragment())
                                    .addToBackStack("Profile").commit();
                            break;
                    }
                    return true;
                });
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(this);
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    private void showDialogMessage(String title, String body, final boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK",
                (dialog, which) -> {
                    try {
                        userDialog.dismiss();
                        if (exit) {
                            exit();
                        }
                    } catch (Exception e) {
                        // Log failure
                        Log.e(TAG, " -- Dialog dismiss failed");
                        if (exit) {
                            exit();
                        }
                    }
                });
        userDialog = builder.create();
        userDialog.show();
    }

    private void closeWaitDialog() {
        try {
            waitDialog.dismiss();
        } catch (Exception e) {
            //
        }
    }

    private void exit() {
        Intent intent = new Intent();
        if (username == null) {
            username = "";
        }
        intent.putExtra("name", username);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            super.onBackPressed();

        }
    }
}

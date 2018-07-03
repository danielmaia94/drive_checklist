package dnx.drive_checklist;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class WriteChecklistActivity extends AppCompatActivity {
    GoogleAccountCredential credential;
    ProgressDialog progress;

    private String user_id;
    private String spreadsheet_id;
    private List<String> answers;
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Retrieves parameters from the Draw Checklist Activity
        Intent i = getIntent();
        answers = (List<String>) i.getSerializableExtra("answers");

        Bundle extras = i.getExtras();
        user_id = extras.getString("user_id");
        spreadsheet_id = extras.getString("spreadsheet_id");

        // Progress
        progress = new ProgressDialog(this);
        progress.setMessage("Enviando respostas...");

        credential = ((CustomApplication) getApplication()).getCredential();

        getResultsFromApi(this);
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                WriteChecklistActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi(Context context)
    {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //output_text.setText("No network connection available.");
        } else {
            new MakeRequestTask(credential, context).execute();
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
                getResultsFromApi(this);
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        credential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<List<Object>>>
    {
        private com.google.api.services.sheets.v4.Sheets service = null;
        private Exception last_error = null;
        private List<List<Object>> users_id = null;
        Context context;

        MakeRequestTask(GoogleAccountCredential credential, Context context)
        {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            service = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();

            // Execution context
            this.context = context;
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<List<Object>> doInBackground(Void... params)
        {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                last_error = e;
                cancel(true);
                return null;
            }
        }

        private String getCharForNumber(int i) {
            return i > 0 && i < 27 ? String.valueOf((char)(i + 64)) : null;
        }

        /**
         * Fetch a checklist from the spreadsheet
         * @return checklist
         * @throws IOException
         */
        private List<List<Object>> getDataFromApi() throws IOException
        {
            String range = "Respostas!A1:A";
            ValueRange response = this.service.spreadsheets().values()
                    .get(spreadsheet_id, range)
                    .execute();
            users_id = response.getValues();

            String cur_user_id;
            int index = 1;
            for (List user_row : users_id) {
                cur_user_id = (String)user_row.get(0);

                // Match on user_id
                if(cur_user_id.equals(user_id)) {
                    List<List<Object>> writeData = new ArrayList<>();
                    List<Object> dataRow = new ArrayList<>();
                    range = "Respostas!B" + Integer.toString(index) + ":" + getCharForNumber(answers.size() + 1);
                    for (String answer: answers) {
                        dataRow.add(answer);
                    }
                    writeData.add(dataRow);

                    List<List<Object>> values = writeData;
                    ValueRange body = new ValueRange()
                            .setValues(values);
                    UpdateValuesResponse result =
                            service.spreadsheets().values().update(spreadsheet_id, range, body)
                                    .setValueInputOption("RAW")
                                    .execute();

                    return writeData;
                }
                index ++;
            }

            return null;
        }

        @Override
        protected void onPreExecute()
        {
            // output_text.setText("");
            progress.show();
        }

        @Override
        protected void onPostExecute(List<List<Object>> output)
        {
            progress.hide();
            if (output == null || output.size() == 0) {
                //output_text.setText("Questionário não encontrado");
            } else {
                finish();
            }
        }

        @Override
        protected void onCancelled()
        {
            progress.hide();
            if (last_error != null) {
                if (last_error instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) last_error)
                                    .getConnectionStatusCode());
                } else if (last_error instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) last_error).getIntent(),
                            ReadChecklistActivity.REQUEST_AUTHORIZATION);
                } else {
//                    output_text.setText("The following error occurred:\n"
//                            + last_error.getMessage());
                }
            } else {
//                output_text.setText("Request cancelled.");
            }
        }
    }
}

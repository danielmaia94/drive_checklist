package dnx.drive_checklist;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    // User's identifier
    private String user_id = "";
    private String spreadsheet_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }

    /**
     * Click event on login button
     */
    public void loginOnClick(View view)
    {
        EditText user_id_edit = findViewById(R.id.user_id);
        user_id =  user_id_edit.getText().toString();

        EditText spreadsheet_id_edit = findViewById(R.id.spreadsheet_id);
        spreadsheet_id = spreadsheet_id_edit.getText().toString();

        // Verifies that both fields have been filled in
        if(user_id != "" && spreadsheet_id != "") {
            Intent intent = new Intent(this, ReadChecklistActivity.class);
            intent.putExtra("user_id", user_id);
            intent.putExtra("spreadsheet_id", spreadsheet_id);
            startActivity(intent);
        }
    }
}
;
package dnx.drive_checklist;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    // User identifier
    String user_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }

    /*
     * Click event on login button
     */
    public void loginOnClick(View view)
    {
        EditText user_id_edit = findViewById(R.id.user_id);
        String user_id =  user_id_edit.getText().toString();

        EditText url_edit = findViewById(R.id.url);
        String url = url_edit.getText().toString();

        if(user_id != "" && url != "") {
            Intent intent = new Intent(this, ChecklistActivity.class);
        }
    }
}
;
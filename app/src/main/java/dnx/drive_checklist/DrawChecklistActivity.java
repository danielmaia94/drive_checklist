package dnx.drive_checklist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DrawChecklistActivity extends AppCompatActivity {
    // Question currently being displayed
    private int current_question = 0;
    private List<List<Object>> sheet_data;
    private List<String> answers =  new ArrayList<String>();
    private String user_id;
    private String spreadsheet_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        Bundle extras = i.getExtras();
        sheet_data = (List<List<Object>>) i.getSerializableExtra("sheet_data");
        user_id = extras.getString("user_id");
        spreadsheet_id = extras.getString("spreadsheet_id");

        setContentView(R.layout.checklist);
        drawQuestion();
    }

    /**
     * Draw the current question from sheet_data
     */
    private void drawQuestion()
    {
        // Get current question
        List<Object> row = sheet_data.get(current_question);
        String question = (String) row.get(0);
        String[] options = ((String) row.get(1)).split(";");

        // Show question text
        TextView question_text = findViewById(R.id.question);
        question_text.setText(question);

        // Show options
        int num_options = options.length < 5 ? options.length : 5;
        String name = getPackageName();
        Resources r = getResources();
        int identifier;
        for (int i = 0; i < 5; i++) {
            identifier = r.getIdentifier("option" + (i + 1), "id", name);
            Button option_button = findViewById(identifier);
            // If not arrived at the final alternative, show the option, else hide
            if(num_options > i) {
                option_button.setText(options[i]);
                option_button.setVisibility(View.VISIBLE);

                // Click Event
                final Context context = this;
                option_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get user's answer
                        Button button = (Button) v;
                        answers.add(button.getText().toString());

                        // Checklist end
                        if(sheet_data.size() == current_question + 1) {
                            Intent intent = new Intent(context, WriteChecklistActivity.class);

                            intent.putExtra("answers", (Serializable)answers);
                            intent.putExtra("user_id", user_id);
                            intent.putExtra("spreadsheet_id", spreadsheet_id);
                            startActivity(intent);
                            finish();
                        } else {
                            // Go to the next question
                            current_question++;
                            drawQuestion();
                        }
                    }
                });
            } else {
                option_button.setVisibility(View.GONE);
            }
        }
    }
}

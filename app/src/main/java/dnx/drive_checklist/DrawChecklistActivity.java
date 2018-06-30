package dnx.drive_checklist;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.List;

public class DrawChecklistActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        List<List<Object>> sheet_data = (List<List<Object>>) i.getSerializableExtra("sheet_data");

        draw(sheet_data);
    }

    /**
     * Draw the Checklist from sheet_data
     */
    private void draw(List<List<Object>> sheet_data)
    {

    }
}

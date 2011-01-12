package sk.gista.android.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.gista.android.maps.R;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.SimpleExpandableListAdapter;

public class Info extends ExpandableListActivity {

	private static final String RECORD = "RECORD";
	private static final String ITEM = "ITEM";
	private static final String DATA = "DATA";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
        for (int i = 1; i <= 3; i++) {
            Map<String, String> curGroupMap = new HashMap<String, String>();
            groupData.add(curGroupMap);
            curGroupMap.put(RECORD, "Record " + i);
            
            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
            for (int j = 1; j <= 10; j++) {
                Map<String, String> curChildMap = new HashMap<String, String>();
                children.add(curChildMap);
                curChildMap.put(ITEM, "Item " + j);
                curChildMap.put(DATA, "This is Android Gisplan demo application. For more information visit www.gista.sk.");
            }
            childData.add(children);
        }
        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
        		this,
        		groupData,
        		R.layout.simple_expandable_list_item_1,
        		new String[] { RECORD },
        		new int[] { R.id.record_title },
        		childData,
        		R.layout.simple_expandable_list_item_2,
        		new String[] { ITEM, DATA },
        		new int[] { R.id.item_title, R.id.data }
        		);
        setListAdapter(adapter);
	}
}

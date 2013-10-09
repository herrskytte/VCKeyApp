package com.vingcard.vingcardkeyapp.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.Country;


public class CountryPickerFragment extends ListFragment implements Comparator<Country> {
	
	public static final String EXTRA_COUNTRY = "com.vingcard.vingcardkeyapp.country";

	private CountryListAdapter adapter;

	/**
	 * Hold all countries, sorted by country name
	 */
	private List<Country> allCountriesList;

	/**
	 * Hold countries that matched user query
	 */
	private List<Country> selectedCountriesList;
	
	private String userQuery = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_country_picker, null);
    }

    @Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Get countries from the json
		initializeCountryList();

		// Set adapter
		adapter = new CountryListAdapter(getActivity(), selectedCountriesList);
		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {		
		Country country = selectedCountriesList.get(position);
		Intent result = new Intent();
		result.putExtra(EXTRA_COUNTRY, country);
		getActivity().setResult(Activity.RESULT_OK, result);
		getActivity().finish();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_search, menu);
		MenuItem mItem = menu.findItem(R.id.menu_search);
		final SearchView searchView = (SearchView) mItem.getActionView();
		searchView.setIconifiedByDefault(false);
		searchView.setQueryHint(getString(R.string.country_search));
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
		    @Override
		    public boolean onQueryTextSubmit(String query) {
		    	InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);				
				imm.hideSoftInputFromWindow(searchView.getApplicationWindowToken(), 0);
		        return true;
		    }
	
		    @Override
		    public boolean onQueryTextChange(String newText) {
		    	search(newText.toLowerCase(Locale.ENGLISH));
		        return true;
		    }
	    });

	}

	/**
	 * Get all countries with code and name from res/raw/countries.json
	 */
	private void initializeCountryList() {
		if (allCountriesList == null) {
			try {
				allCountriesList = readCountriesFromFile();

				// Sort the all countries list based on country name
				Collections.sort(allCountriesList, this);

				// Initialize selected countries with all countries
				selectedCountriesList = new ArrayList<Country>();
				selectedCountriesList.addAll(allCountriesList);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Convenient function to read from raw file
	 */
	private List<Country> readCountriesFromFile() {
		InputStream inputStream = getActivity().getResources().openRawResource(
				R.raw.countries);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream));

		Gson gson = new Gson(); 
		Type listType = new TypeToken<List<Country>>(){}.getType();
		return gson.fromJson(reader, listType);
	}

	/**
	 * Search allCountriesList contains text and put result into
	 * selectedCountriesList
	 * 
	 * @param text Must be lower-case
	 */
	private void search(String text) {
		userQuery = text;
		selectedCountriesList.clear();

		for (Country country : allCountriesList) {
			if (country.getName().toLowerCase(Locale.ENGLISH).contains(text)) {
				selectedCountriesList.add(country);
			}
		}
		
		Collections.sort(selectedCountriesList, this);

		adapter.notifyDataSetChanged();
	}

	/**
	 * Support sorting the countries list
	 */
	@Override
	public int compare(Country lhs, Country rhs) {
		if(!userQuery.isEmpty()){
			Integer leftIndex = lhs.getName().toLowerCase(Locale.ENGLISH).indexOf(userQuery);
			Integer rightIndex = rhs.getName().toLowerCase(Locale.ENGLISH).indexOf(userQuery);
			if((int)leftIndex != rightIndex){
				return leftIndex.compareTo(rightIndex);
			}
		}
		return lhs.getName().compareTo(rhs.getName());

	}

    enum ViewType {
        SINGLE, TOP, BOTTOM, MIDDLE
    }

	private class CountryListAdapter extends BaseAdapter {
        private Context context;
        List<Country> countries;

		LayoutInflater inflater;

		public CountryListAdapter(Context context, List<Country> countries) {
			super();
			this.context = context;
			this.countries = countries;
			inflater = (LayoutInflater) this.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return countries.size();
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View cellView = null;

            ViewType currentType;
            if(getCount() == 1){
                currentType = ViewType.SINGLE;
            }else if(position == 0){
                currentType = ViewType.TOP;
            }else if(position == getCount() - 1){
                currentType = ViewType.BOTTOM;
            }else{
                currentType = ViewType.MIDDLE;
            }

            //Attempt to reuse view
			if(convertView != null){
                ViewType convertType;
                if(convertView.getId() == R.id.list_item_middle){
                    convertType = ViewType.MIDDLE;
                }else if(convertView.getId() == R.id.list_item_top){
                    convertType = ViewType.TOP;
                }else if(convertView.getId() == R.id.list_item_bottom){
                    convertType = ViewType.BOTTOM;
                }else{
                    convertType = ViewType.SINGLE;
                }
                if(convertType == currentType){
                    cellView = convertView;
                }
            }

			if (cellView == null) {
                if(currentType == ViewType.SINGLE){
                    cellView = inflater.inflate(R.layout.country_picker_row_single, null);
                }else if(currentType == ViewType.TOP){
                    cellView = inflater.inflate(R.layout.country_picker_row_top, null);
                }else if(currentType == ViewType.BOTTOM){
                    cellView = inflater.inflate(R.layout.country_picker_row_bottom, null);
                }else{
                    cellView = inflater.inflate(R.layout.country_picker_row, null);
                }
			}

            Country country = countries.get(position);

			TextView textView = (TextView) cellView.findViewById(R.id.row_title);
			ImageView imageView = (ImageView) cellView.findViewById(R.id.row_icon);
			
			textView.setText(country.getName() + " (+" + country.getPhoneCode() + ")");

			// Load drawable dynamically from country code
			imageView.setImageResource(country.getImageResId());
			return cellView;
		}
	}
}
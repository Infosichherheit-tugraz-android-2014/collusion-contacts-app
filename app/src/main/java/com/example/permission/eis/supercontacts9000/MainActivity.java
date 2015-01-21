package com.example.permission.eis.supercontacts9000;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ContactListFragment())
                    .commit();

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_backup) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ContactListFragment extends Fragment {


        public ContactListFragment() {
        }

        /* Checks if external storage is available for read and write */
        private static boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

        /* Checks if external storage is available to at least read */
        private static boolean isExternalStorageReadable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) ||
                    Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                return true;
            }
            return false;
        }



        private static List<List<String>> getAllContacts(Activity myActivity) {

            ContentResolver cr = myActivity.getContentResolver();
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
            List<List<String>> contacts = new ArrayList<>();

            if (cur.getCount() <= 0 ) {
                cur.close();
                return contacts;
            }

            while (cur.moveToNext()) {
                String name = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                int hasPhone = Integer.parseInt(
                        cur.getString(
                                cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhone == 1) {
                    String contactId =
                            cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                    while (phones.moveToNext()) {
                        String number = phones.getString(phones.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        List currentContact = new ArrayList<>();
                        currentContact.add(0, name);
                        currentContact.add(1, number);
                        contacts.add(currentContact);
                        break;
                    }
                    phones.close();
                }
            }
            cur.close();

            return contacts;
        }

        private static List<List<String>> writeContactsToDisk(Activity myActivity) {

            List<List<String>> contacts = getAllContacts(myActivity);

            String fullFile = "";

            if (isExternalStorageReadable() && isExternalStorageWritable()) {

                for (List<String> c : contacts.subList(0, 100)) {
                    if (c.get(0) != null) {
                        String line = c.get(0).concat("\t").concat(c.get(1)).concat("\n");
                        fullFile = fullFile.concat(line);
                    }
                }
            }

            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS);
            File file = new File(path, "contacts_bu_.txt");

            try {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                BufferedWriter writer = new BufferedWriter(osw);
                writer.write(fullFile, 0, fullFile.length());
                writer.flush();
                fos.close();
                osw.close();
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return contacts;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            myProgress = new ProgressDialog(getActivity());
            myProgress.setTitle("Loading");
            myProgress.setMessage("Wait while loading...");
            myProgress.setCancelable(false);
            myProgress.setCanceledOnTouchOutside(false);
            myProgress.show();
            myListView = (ListView) rootView.findViewById(R.id.contactsListView);

            new WriteContactsToDisk().execute();

            return rootView;
        }

        private ListView myListView;
        private ProgressDialog myProgress;

        private class WriteContactsToDisk extends AsyncTask<Object[], Object[], List<String>> {

            @Override
            protected List<String> doInBackground(Object[]... params) {
                List<List<String>> contacts = writeContactsToDisk(getActivity());
                List<String> formattedContacts = new ArrayList<String>();

                for (List<String> c : contacts) {
                    String tmpString = "";
                    tmpString = tmpString.concat(c.get(0)).concat("\n").concat(c.get(1));
                    formattedContacts.add(tmpString);
                }
                return formattedContacts;
            }

            @Override
            protected void onPostExecute(List<String> formattedContacts) {
                ArrayAdapter<String> contactAdapter = new ArrayAdapter<String>(getActivity(),
                        R.layout.list_item_contacts,
                        R.id.list_item_contact_textview,
                        formattedContacts);
                myListView.setAdapter(contactAdapter);
                myProgress.dismiss();

            }
        }
    }
}



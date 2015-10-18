package debut.jeudeloie;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;

import debut.jeudeloie.commun.AdapteurJoueurs;
import debut.jeudeloie.commun.Declarations;
import debut.jeudeloie.commun.DonneesJoueur;

public class ActiviteTVResultats extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_resultats);
        String jsd = getIntent().getStringExtra(Declarations.TAG_JSON_DONNEES);
        Log.d(Declarations.TAG_DEBUG, "ActiviteTVResultats : donnees : " + jsd);
        ArrayList<DonneesJoueur> lj = DonneesJoueur.fromjsonliste(jsd);
        AdapteurJoueurs adapt = new AdapteurJoueurs(this, R.layout.vuejoueur);
        adapt.addAll(lj);
        ((ListView) findViewById(R.id.listeResultatsJoueurs)).setAdapter(adapt);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menuresultats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

package debut.jeudeloie.commun;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;

/**
 * Created by robert on 17/07/15.
 */
public class AdapteurJoueurs extends ArrayAdapter<DonneesJoueur> {

    private Context monContexte;
    private int vuelayout;

    public AdapteurJoueurs(Context context, int resource) {
        super(context, resource);
        monContexte = context;
        vuelayout = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vue = LayoutInflater.from(monContexte).inflate(vuelayout, null);
        DonneesJoueur jou = this.getItem(position);
        ((TextView) vue.findViewById(R.id.vuejoueurnumero)).setText(String.valueOf(String.valueOf(jou.getNumero())));
        ((TextView) vue.findViewById(R.id.vuejoueurtexte)).setText(jou.getNomComplet());
        Picasso.with(monContexte).load(jou.getUrllogo()).error(R.drawable.teteoie).placeholder(R.drawable.teteoie)
                .into((ImageView) vue.findViewById(R.id.vuejoueurimage));
        ((TextView) vue.findViewById(R.id.vuejoueurposition)).setText(String.valueOf(jou.getPosition()));
        if(jou.isGagnant()) {
            ((ImageView) vue.findViewById(R.id.vueiconegagnant)).setImageResource(R.drawable.gagnant);
        } else {
            ((ImageView) vue.findViewById(R.id.vueiconegagnant)).setImageBitmap(null);
        }
        if(jou.getTourspenalite() > 0) {
            ((TextView) vue.findViewById(R.id.vuepenalitevaleur)).setText(String.valueOf(jou.getTourspenalite()));
            vue.findViewById(R.id.vuepenalitevaleur).setVisibility(View.VISIBLE);
            vue.findViewById(R.id.vuepenalitetitre).setVisibility(View.VISIBLE);
        } else {
            vue.findViewById(R.id.vuepenalitevaleur).setVisibility(View.INVISIBLE);
            vue.findViewById(R.id.vuepenalitetitre).setVisibility(View.INVISIBLE);
        }
        return vue;
    }
}

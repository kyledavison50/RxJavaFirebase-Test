package com.example.rxjavafirebase_test;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private String TAG = "rxjavaQ";
    private Button button;

    private FirebaseFirestore firestoreDB;

    private CompositeDisposable mycompositeDisposable = new CompositeDisposable();
    private DisposableObserver<List<String>> mydisposableObserver;
    private Observable<List<String>> stringObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
        firestoreDB = FirebaseFirestore.getInstance();

        button.setOnClickListener(v -> {
            addNewCity();
        });

        addCities();
        acquireInfo();
    }

    public void acquireInfo() {
        stringObservable = Observable.create(emitter -> {
            try {
                getCities(emitter);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });

        // Without using a declared disposable observer, subscribe contains OnNext functionality
/*        mycompositeDisposable.add(
                stringObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(string -> textView.setText(textView.getText().toString() + string),
                                   throwable -> {  }));*/

        // With a dedicated Disposable Observer, use .subscribeWith(*Observer Here*)
        mycompositeDisposable.add(
                stringObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(getObserver()));

        // Example if I wanted to use a map for a List
        /*mycompositeDisposable.add(
                stringObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(new Function<List<String>, Object>() {
                            @Override
                            public Object apply(List<String> strings) throws Exception {
                                return null;
                            }
                        })
                        .subscribe());*/
    }

    public void getCities(ObservableEmitter emitter) {

        // Create a query directly from FireStore object, shortest way, no realtime updates, manually call updates
        // Wont work, change to handle List<String> not String, then it will work
/*        firestoreDB.collection("cities")
                .whereEqualTo("capital", true)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Retrieved Data " + document.getData());
                            City city = document.toObject(City.class);
                            emitter.onNext(city.state);
                        }
                    }
                    else{
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });*/

        // Realtime updates, when the observed documents change, it will update here (call the emitter)
        firestoreDB.collection("cities").whereEqualTo("capital", true)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        Log.w(TAG, "Snap shots are null");
                        return;
                    }

                    List<String> cities = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d(TAG, "Retrieved Data " + document.getData());
                        City city = document.toObject(City.class);
                        cities.add(city.getCountry());
                    }
                    emitter.onNext(cities);
                });
    }

    public DisposableObserver<List<String>> getObserver() {
        return mydisposableObserver = new DisposableObserver<List<String>>() {
            @Override
            public void onNext(List<String> strings) {
                textView.setText("");
                for (String string : strings) {
                    textView.setText(textView.getText().toString() + string);
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mycompositeDisposable.clear();
    }

    public void addCities() {
        // One way using a hash map
        /*CollectionReference cities = firestoreDB.collection("cities");

        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "San Francisco");
        data1.put("state", "CA");
        data1.put("country", "USA");
        data1.put("capital", false);
        data1.put("population", 860000);
        data1.put("regions", Arrays.asList("west_coast", "norcal"));
        cities.document("SF").set(data1);*/

        // POJO class way
        City city = new City("Rogers", "USA", "AR", true, 400000L, Arrays.asList("central", "central"));
        firestoreDB.collection("cities").document(city.getState()).set(city);
    }

    public void addNewCity() {
        final Random myRandom = new Random();

        City city = new City("Lowell", "NEWWWW", "AR" + myRandom.nextInt(100), true, 100000L, Arrays.asList("central", "central"));
        firestoreDB.collection("cities").document(city.getState()).set(city);
    }

    public static class City {
        private String name;
        private String country;
        private String state;
        private boolean capital;
        private long population;
        private List<String> regions;

        public City() {
        }

        public City(String name, String country, String state, boolean capital, long population, List<String> regions) {
            this.name = name;
            this.country = country;
            this.state = state;
            this.capital = capital;
            this.population = population;
            this.regions = regions;
        }

        public String getName() {
            return name;
        }

        public String getState() {
            return state;
        }

        public String getCountry() {
            return country;
        }

        public boolean isCapital() {
            return capital;
        }

        public long getPopulation() {
            return population;
        }

        public List<String> getRegions() {
            return regions;
        }

    }
}

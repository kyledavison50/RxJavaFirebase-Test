package com.example.rxjavafirebase_test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private String TAG = "rxjavaQ";

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private CompositeDisposable mycompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("message");

        acquireInfo();
    }

    public void acquireInfo(){
        Observable<String> stringObservable = Observable.create(emitter -> {
            try {
                getStrings(emitter);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });

        mycompositeDisposable.add(
                stringObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(string -> textView
                                .setText(textView.getText().toString() + string),
                                        throwable -> {  }));
    }

    public void getStrings(ObservableEmitter<String> emitter){
        myRef = database.getReference();
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String message = postSnapshot.getValue(String.class);
                    Log.v(TAG, "snapshot = " + message);
                    emitter.onNext(message);
                }
                emitter.onComplete();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mycompositeDisposable.clear();
    }
}

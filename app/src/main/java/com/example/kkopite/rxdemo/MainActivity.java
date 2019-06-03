package com.example.kkopite.rxdemo;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class MainActivity extends AppCompatActivity {

    private Disposable mDisposable;
    private Toast mToast;

    private static final String TAG = "MainActivity";

    private boolean status = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 这样在每个item之间都是小于两秒间隔 称之为一个动作流
        // 就可以用来做两次点击退出(第一次弹出提示, 第二次真正弹出)
        // 而如果两个item间隔大于2s, 则会变成不同的两个动作流, 即都是执行第一次弹出提示

        // 同时也可以用来做类似开发者模式, 点击7次后才开始开发者模式

        // 用debounce 来模拟说将 间隔小于2s的item打包在一起
        // 因为dubounce 必须是在一个item发射后, 2s内无item发射, 才会发射一个,
        // 故可以用来这么干

        // 将原序列变成(1, 1, 1, ....)
        // 和重置item合并
        mDisposable = mBackClick.mergeWith(mBackClick.debounce(2000, TimeUnit.MILLISECONDS).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer aVoid) throws Exception {
                // 这里没有运行到? 为毛
                Log.e(TAG, "apply: 重置位");
                return 0;
            }
            // 变成 (1, 1, 1, 0, 1, 1, 0, ..)
            // 使用scan去转化, 前面值为0, 就是1, 否则就是前面的值加一
            // (1 , 2, 3, 0, 1, 2, 0 ...)
        })).scan(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer integer, Integer integer2) throws Exception {
                // 前一个是重置位, 故从1开始算
//                if (integer == 0) return 1;
                // 此时item是重置位, 不变还是0, 方便后面过滤掉重置位事件
                if (integer2 == 0) return 0;
                // 事件递增
                return integer + 1;
            }
            // 吧重置item给过滤掉
        }).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) throws Exception {
                return integer > 0;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e(TAG, "apply: " + integer);
//                String msg = "第" + integer + "次";
//                toast(msg);
                if (integer == 1) {
                    toast("再点击一次返回退出");
                } else {
                    MainActivity.super.onBackPressed();
                }
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBackClick.onNext(1);
            }
        });
    }

    private Subject<Integer> mBackClick = PublishSubject.create();

    private void toast(final String string) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT);
        mToast.show();
    }

    @Override
    public void onBackPressed() {
        mBackClick.onNext(1);
    }
}

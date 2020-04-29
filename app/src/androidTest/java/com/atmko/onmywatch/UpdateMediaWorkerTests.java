package com.atmko.onmywatch;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.testing.TestWorkerBuilder;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class UpdateMediaWorkerTests {
    private final Context context = ApplicationProvider.getApplicationContext();
    private Executor mExecutor;
    private AppDatabase mDb;

    @Before
    public void setUp() {
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Before
    public void setupTestDatabase() {
        mDb = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .build();

        AppDatabase.setDatabase(mDb);
    }

    @Before
    public void populateDatabase() {
        String[] watchStatusTitles = context.getResources()
                .getStringArray(R.array.watch_status_titles);

        //starting from 1 skips 0(none watch status)
        for (int i = 1; i < watchStatusTitles.length; i++) {
            WatchListModel watchListModel = new WatchListModel(watchStatusTitles[i]);
            AppDatabase.getInstance(context).watchListsDao().addList(watchListModel);
        }
    }

    //todo: add last updated media data property to test strings
    public void injectMovieDetailsString() {
        UpdateMediaWorker.sMovieDetailsStringInject = "{\n" +
                "  \"adult\": true,\n" +
                "  \"backdrop_path\": \"/weeee!!!\",\n" +
                "  \"belongs_to_collection\": {\n" +
                "    \"id\": 10,\n" +
                "    \"name\": \"Star Wars Collection\",\n" +
                "    \"poster_path\": \"/iTQHKziZy9pAAY4hHEDCGPaOvFC.jpg\",\n" +
                "    \"backdrop_path\": \"/d8duYyyC9J5T825Hg7grmaabfxQ.jpg\"\n" +
                "  },\n" +
                "  \"budget\": 0,\n" +
                "  \"genres\": [\n" +
                "    {\n" +
                "      \"id\": 28,\n" +
                "      \"name\": \"Action\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 12,\n" +
                "      \"name\": \"Adventure\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"homepage\": \"https://www.starwars.com/films/star-wars-episode-ix-the-rise-of-skywalker\",\n" +
                "  \"id\": 181812,\n" +
                "  \"imdb_id\": \"tt2527338\",\n" +
                "  \"original_language\": \"en\",\n" +
                "  \"original_title\": \"Star Wars: The Rise of Skywalker\",\n" +
                "  \"overview\": \"overview\",\n" +
                "  \"popularity\": 5000,\n" +
                "  \"poster_path\": \"/nawnaw\",\n" +
                "  \"production_companies\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"logo_path\": \"/o86DbpburjxrqAzEDhXZcyE8pDb.png\",\n" +
                "      \"name\": \"Lucasfilm\",\n" +
                "      \"origin_country\": \"US\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 11461,\n" +
                "      \"logo_path\": \"/p9FoEt5shEKRWRKVIlvFaEmRnun.png\",\n" +
                "      \"name\": \"Bad Robot\",\n" +
                "      \"origin_country\": \"US\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"logo_path\": \"/wdrCwmRnLFJhEoH8GSfymY85KHT.png\",\n" +
                "      \"name\": \"Walt Disney Pictures\",\n" +
                "      \"origin_country\": \"US\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"production_countries\": [\n" +
                "    {\n" +
                "      \"iso_3166_1\": \"US\",\n" +
                "      \"name\": \"United States of America\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"release_date\": \"2028-12-18\",\n" +
                "  \"revenue\": 0,\n" +
                "  \"runtime\": 141,\n" +
                "  \"spoken_languages\": [\n" +
                "    {\n" +
                "      \"iso_639_1\": \"en\",\n" +
                "      \"name\": \"English\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"status\": \"Released\",\n" +
                "  \"tagline\": \"Every generation has a legend\",\n" +
                "  \"title\": \"Star Wars: The Rise of Skywalker\",\n" +
                "  \"video\": true,\n" +
                "  \"vote_average\": 6.7,\n" +
                "  \"vote_count\": 289,\n" +
                "  \"credits\": {\n" +
                "    \"cast\": [\n" +
                "      {\n" +
                "        \"cast_id\": 25,\n" +
                "        \"character\": \"General Leia Organa\",\n" +
                "        \"credit_id\": \"5ad70169c3a36847d5008556\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 4,\n" +
                "        \"name\": \"Carrie Fisher\",\n" +
                "        \"order\": 0,\n" +
                "        \"profile_path\": \"/rfJtncHewKVnHjqpIZvjn24ESeC.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 30,\n" +
                "        \"character\": \"Luke Skywalker\",\n" +
                "        \"credit_id\": \"5b5b8d12c3a368421b005b72\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Mark Hamill\",\n" +
                "        \"order\": 1,\n" +
                "        \"profile_path\": \"/fk8OfdReNltKZqOk2TZgkofCUFq.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 22,\n" +
                "        \"character\": \"Kylo Ren\",\n" +
                "        \"credit_id\": \"5a515667925141132c0123fb\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1023139,\n" +
                "        \"name\": \"Adam Driver\",\n" +
                "        \"order\": 2,\n" +
                "        \"profile_path\": \"/rsjwgpV2OukxOJ9HEiEyf4qu1vR.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 89,\n" +
                "        \"character\": \"Rey\",\n" +
                "        \"credit_id\": \"5cba346f925141097df465fe\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1315036,\n" +
                "        \"name\": \"Daisy Ridley\",\n" +
                "        \"order\": 3,\n" +
                "        \"profile_path\": \"/oP7khxQ6BUNoGiPFUCI8dikN0ew.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 90,\n" +
                "        \"character\": \"Finn\",\n" +
                "        \"credit_id\": \"5cba34ddc3a3683abf85d6e7\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 236695,\n" +
                "        \"name\": \"John Boyega\",\n" +
                "        \"order\": 4,\n" +
                "        \"profile_path\": \"/idr2vphzhidyMGOp0ky6RqUMah8.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 23,\n" +
                "        \"character\": \"Poe Dameron\",\n" +
                "        \"credit_id\": \"5a7814c3c3a368226100d05d\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 25072,\n" +
                "        \"name\": \"Oscar Isaac\",\n" +
                "        \"order\": 5,\n" +
                "        \"profile_path\": \"/cY6ail3ebXDx9FCoZMgVGAbmBus.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 33,\n" +
                "        \"character\": \"C-3PO\",\n" +
                "        \"credit_id\": \"5b5f428dc3a368356d0014c4\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 6,\n" +
                "        \"name\": \"Anthony Daniels\",\n" +
                "        \"order\": 6,\n" +
                "        \"profile_path\": \"/cljvryjb3VwTsNR7fjQKjNPMaBB.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 36,\n" +
                "        \"character\": \"Jannah\",\n" +
                "        \"credit_id\": \"5b5f42a90e0a261cf40010e6\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1537686,\n" +
                "        \"name\": \"Naomi Ackie\",\n" +
                "        \"order\": 7,\n" +
                "        \"profile_path\": \"/vVHuadobY2lFRSYFJJK91T0WbZ9.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 32,\n" +
                "        \"character\": \"General Hux\",\n" +
                "        \"credit_id\": \"5b5f427bc3a368356b00144c\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 93210,\n" +
                "        \"name\": \"Domhnall Gleeson\",\n" +
                "        \"order\": 8,\n" +
                "        \"profile_path\": \"/nSZ0JZEvHnEJ3sxQ8TWwOvQlMSo.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 35,\n" +
                "        \"character\": \"Allegiant General Pryde\",\n" +
                "        \"credit_id\": \"5b5f42a29251414fb5000fa5\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 20766,\n" +
                "        \"name\": \"Richard E. Grant\",\n" +
                "        \"order\": 9,\n" +
                "        \"profile_path\": \"/6UXv8E4WWvRCKMQx1FQ0FJVyu0a.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 26,\n" +
                "        \"character\": \"Maz Kanata\",\n" +
                "        \"credit_id\": \"5ad70195c3a368480b009b26\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1267329,\n" +
                "        \"name\": \"Lupita Nyong'o\",\n" +
                "        \"order\": 10,\n" +
                "        \"profile_path\": \"/l8RXyGKcCqEagjEXOm8iUaQtKdW.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 28,\n" +
                "        \"character\": \"Zorii Bliss\",\n" +
                "        \"credit_id\": \"5b3ff10cc3a368073d00045a\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 41292,\n" +
                "        \"name\": \"Keri Russell\",\n" +
                "        \"order\": 11,\n" +
                "        \"profile_path\": \"/1K9x70SeXH8aXwMN3pedtVpv1gn.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 27,\n" +
                "        \"character\": \"Chewbacca\",\n" +
                "        \"credit_id\": \"5ad702020e0a2674c7009b16\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1709041,\n" +
                "        \"name\": \"Joonas Suotamo\",\n" +
                "        \"order\": 12,\n" +
                "        \"profile_path\": \"/qzUsjyKkh1G07SFSsXwo4t7vGpv.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 31,\n" +
                "        \"character\": \"Rose Tico\",\n" +
                "        \"credit_id\": \"5b5f42689251414fb800115b\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1663195,\n" +
                "        \"name\": \"Kelly Marie Tran\",\n" +
                "        \"order\": 13,\n" +
                "        \"profile_path\": \"/2YuymbQfIlaUx8xtAxL5OOCsw6H.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 87,\n" +
                "        \"character\": \"Emperor Palpatine\",\n" +
                "        \"credit_id\": \"5cb0c7ca0e0a2626cec4d857\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 27762,\n" +
                "        \"name\": \"Ian McDiarmid\",\n" +
                "        \"order\": 14,\n" +
                "        \"profile_path\": \"/cqEAblt0KJRIGyMXhj5OM5WJ9SN.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 29,\n" +
                "        \"character\": \"Lando Calrissian\",\n" +
                "        \"credit_id\": \"5b459f720e0a26605300192c\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 3799,\n" +
                "        \"name\": \"Billy Dee Williams\",\n" +
                "        \"order\": 15,\n" +
                "        \"profile_path\": \"/sDuo82Mb5o3ZGt4SuV9dR0lAh8P.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 139,\n" +
                "        \"character\": \"Han Solo\",\n" +
                "        \"credit_id\": \"5dfa4449d1a893001480b8e1\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 3,\n" +
                "        \"name\": \"Harrison Ford\",\n" +
                "        \"order\": 16,\n" +
                "        \"profile_path\": \"/7CcoVFTogQgex2kJkXKMe8qHZrC.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 38,\n" +
                "        \"character\": \"Beaumont Kin\",\n" +
                "        \"credit_id\": \"5b807a14c3a36865580006aa\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1330,\n" +
                "        \"name\": \"Dominic Monaghan\",\n" +
                "        \"order\": 17,\n" +
                "        \"profile_path\": \"/2Yw46GNUDD3jZs5VNcjwvsU7vEX.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 34,\n" +
                "        \"character\": \"Lieutenant Connix\",\n" +
                "        \"credit_id\": \"5b5f429c0e0a261cf00012f4\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1399531,\n" +
                "        \"name\": \"Billie Lourd\",\n" +
                "        \"order\": 18,\n" +
                "        \"profile_path\": \"/ZNvGBzuLyVYzqHlWDFmBjaCW2E.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 41,\n" +
                "        \"character\": \"Snap Wexley\",\n" +
                "        \"credit_id\": \"5b90fbe10e0a26659c00c8d5\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 17305,\n" +
                "        \"name\": \"Greg Grunberg\",\n" +
                "        \"order\": 19,\n" +
                "        \"profile_path\": \"/kFK6AaPoUlLJArwLuHetUav8q19.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 150,\n" +
                "        \"character\": \"Commander D'Acy\",\n" +
                "        \"credit_id\": \"5dfaaa16528b2e001804a70e\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 990064,\n" +
                "        \"name\": \"Amanda Lawrence\",\n" +
                "        \"order\": 20,\n" +
                "        \"profile_path\": \"/n0RmDWYo266XvrNlS0aZCXSOihn.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 43,\n" +
                "        \"character\": \"BB-8\",\n" +
                "        \"credit_id\": \"5bbdd2160e0a266606030115\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1234529,\n" +
                "        \"name\": \"Dave Chapman\",\n" +
                "        \"order\": 21,\n" +
                "        \"profile_path\": \"/x7wK7qC6wekaaGHt0Zz9z9qCr8C.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 42,\n" +
                "        \"character\": \"BB-8\",\n" +
                "        \"credit_id\": \"5b90fbf50e0a26659600d2f4\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1329041,\n" +
                "        \"name\": \"Brian Herring\",\n" +
                "        \"order\": 22,\n" +
                "        \"profile_path\": \"/hFCRdsUUvN7TOi7vf3daWNjMF5R.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 95,\n" +
                "        \"character\": \"\",\n" +
                "        \"credit_id\": \"5daf14891cac8c00150c2fa2\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 19903,\n" +
                "        \"name\": \"Richard Bremmer\",\n" +
                "        \"order\": 23,\n" +
                "        \"profile_path\": \"/eRLxQ7sTWqtI24OPo5wz3p95RI3.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 94,\n" +
                "        \"character\": \"General\",\n" +
                "        \"credit_id\": \"5daf1479a44d09001382d076\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 946350,\n" +
                "        \"name\": \"Nasser Memarzia\",\n" +
                "        \"order\": 25,\n" +
                "        \"profile_path\": \"/sbcpt3SJhieMDl5umrxmSmAz5sF.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 93,\n" +
                "        \"character\": \"General Engell\",\n" +
                "        \"credit_id\": \"5daf145db5bc21001472702c\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 151779,\n" +
                "        \"name\": \"Simon Paisley Day\",\n" +
                "        \"order\": 26,\n" +
                "        \"profile_path\": \"/85s4XMpNWHwQzDy6I0wNTV3TsA9.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 140,\n" +
                "        \"character\": \"Anakin Skywalker (voice)\",\n" +
                "        \"credit_id\": \"5dfa44ac609750001532d0d6\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 17244,\n" +
                "        \"name\": \"Hayden Christensen\",\n" +
                "        \"order\": 28,\n" +
                "        \"profile_path\": \"/lz6mI2hhVrGu640fxm9GQkv4V7l.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 141,\n" +
                "        \"character\": \"Obi-Wan Kenobi (voice)\",\n" +
                "        \"credit_id\": \"5dfa44be26dac10014597a9f\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 3061,\n" +
                "        \"name\": \"Ewan McGregor\",\n" +
                "        \"order\": 29,\n" +
                "        \"profile_path\": \"/aEmyadfRXTmmR7UW7OXsm5a6smS.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 148,\n" +
                "        \"character\": \"Obi-Wan Kenobi (archive voice)\",\n" +
                "        \"credit_id\": \"5dfaa95026dac1001759e1a5\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 12248,\n" +
                "        \"name\": \"Alec Guinness\",\n" +
                "        \"order\": 30,\n" +
                "        \"profile_path\": \"/iC1SFEjISE1xMq4HMZHh3lBShzy.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 142,\n" +
                "        \"character\": \"Qui-Gon Jinn (voice)\",\n" +
                "        \"credit_id\": \"5dfa44e2d1a893001480b985\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 3896,\n" +
                "        \"name\": \"Liam Neeson\",\n" +
                "        \"order\": 31,\n" +
                "        \"profile_path\": \"/oxCCVmDSxWcqIyMknRoOAZkvb6D.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 143,\n" +
                "        \"character\": \"Mace Windu (voice)\",\n" +
                "        \"credit_id\": \"5dfaa8f1528b2e001804a582\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 2231,\n" +
                "        \"name\": \"Samuel L. Jackson\",\n" +
                "        \"order\": 32,\n" +
                "        \"profile_path\": \"/qjYcO8jKNlb7lnJ05vh2U7lNt8r.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 144,\n" +
                "        \"character\": \"Yoda (voice)\",\n" +
                "        \"credit_id\": \"5dfaa8fc6097500012339047\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 7908,\n" +
                "        \"name\": \"Frank Oz\",\n" +
                "        \"order\": 33,\n" +
                "        \"profile_path\": \"/9KqCa2wS4EO2yymrVIgMiFHh6M4.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 145,\n" +
                "        \"character\": \"Snoke (voice)\",\n" +
                "        \"credit_id\": \"5dfaa908609750001233904b\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1333,\n" +
                "        \"name\": \"Andy Serkis\",\n" +
                "        \"order\": 34,\n" +
                "        \"profile_path\": \"/2aJLwOQK50Lx7PvIHGW1GMytTOL.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 146,\n" +
                "        \"character\": \"Ahsoka Tano (voice)\",\n" +
                "        \"credit_id\": \"5dfaa9236097500012339058\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 41345,\n" +
                "        \"name\": \"Ashley Eckstein\",\n" +
                "        \"order\": 35,\n" +
                "        \"profile_path\": \"/aEAQPWFv2mlH2Pp9TPJMfIDnIOJ.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 147,\n" +
                "        \"character\": \"Luminaria Unduli (voice)\",\n" +
                "        \"credit_id\": \"5dfaa93965686e00158cbbbb\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 46423,\n" +
                "        \"name\": \"Olivia d'Abo\",\n" +
                "        \"order\": 36,\n" +
                "        \"profile_path\": \"/l7m7nw7wQFvc2ROXOTa4Flblh4M.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 149,\n" +
                "        \"character\": \"Aayla Secura (voice)\",\n" +
                "        \"credit_id\": \"5dfaa9975ed9620016e39181\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 81667,\n" +
                "        \"name\": \"Jennifer Hale\",\n" +
                "        \"order\": 37,\n" +
                "        \"profile_path\": \"/vdU4NPiUGTksctN6IrxMP3nXxgR.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 151,\n" +
                "        \"character\": \"Darth Vader (voice)\",\n" +
                "        \"credit_id\": \"5dfaaa5cd1a893001981b9a6\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15152,\n" +
                "        \"name\": \"James Earl Jones\",\n" +
                "        \"order\": 38,\n" +
                "        \"profile_path\": \"/oqMPIsXrl9SZkRfIKN08eFROmH6.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 152,\n" +
                "        \"character\": \"Kanan Jarrus (voice)\",\n" +
                "        \"credit_id\": \"5dfaaab4528b2e001504b337\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 33260,\n" +
                "        \"name\": \"Freddie Prinze Jr.\",\n" +
                "        \"order\": 39,\n" +
                "        \"profile_path\": \"/xkbGC7tDwoKUwpNcOLsNUMjLjP4.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 153,\n" +
                "        \"character\": \"Oma Tres\",\n" +
                "        \"credit_id\": \"5dfaab5b528b2e001504b4b2\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 491,\n" +
                "        \"name\": \"John Williams\",\n" +
                "        \"order\": 40,\n" +
                "        \"profile_path\": \"/2Ats98PB1SH2yfEPikiLdhRuXZm.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 154,\n" +
                "        \"character\": \"Wedge Antilles\",\n" +
                "        \"credit_id\": \"5dfae8db6097500012340ec9\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 47698,\n" +
                "        \"name\": \"Denis Lawson\",\n" +
                "        \"order\": 41,\n" +
                "        \"profile_path\": \"/nxPR7KIlJ4CPPI2hniTUk6bu9fA.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 155,\n" +
                "        \"character\": \"Wicket W. Warrick\",\n" +
                "        \"credit_id\": \"5dfae9095ed9620013e438d8\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 11184,\n" +
                "        \"name\": \"Warwick Davis\",\n" +
                "        \"order\": 42,\n" +
                "        \"profile_path\": \"/4LjgmjD9nKOgL3gGRhIS5EkI0a.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 156,\n" +
                "        \"character\": \"Babu Frik\",\n" +
                "        \"credit_id\": \"5dfae9536097500012340f70\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1834,\n" +
                "        \"name\": \"Shirley Henderson\",\n" +
                "        \"order\": 43,\n" +
                "        \"profile_path\": \"/dxVvLjGNKVMl5lDXXInJ7xWpCvl.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 157,\n" +
                "        \"character\": \"Lieutenant Garan\",\n" +
                "        \"credit_id\": \"5dfae97e609750002134233c\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1399747,\n" +
                "        \"name\": \"Mandeep Dhillon\",\n" +
                "        \"order\": 44,\n" +
                "        \"profile_path\": \"/yccJG9wqo2xEWi5tlmC9EPrVHsO.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 158,\n" +
                "        \"character\": \"D-O (voice)\",\n" +
                "        \"credit_id\": \"5dfae99560975000213423a4\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15344,\n" +
                "        \"name\": \"J.J. Abrams\",\n" +
                "        \"order\": 45,\n" +
                "        \"profile_path\": \"/h0o12c399M5hTKWl0qdgwhaddNt.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 159,\n" +
                "        \"character\": \"Pilot Vanik\",\n" +
                "        \"credit_id\": \"5dfae9cbd1a893001481dc2f\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 43554,\n" +
                "        \"name\": \"Josef Altin\",\n" +
                "        \"order\": 46,\n" +
                "        \"profile_path\": \"/lnr6IQUAyXP0y1oj7sEZgmNpzpL.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 160,\n" +
                "        \"character\": \"FN-2802\",\n" +
                "        \"credit_id\": \"5dfaea0d609750001533f450\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 957038,\n" +
                "        \"name\": \"Nigel Godrich\",\n" +
                "        \"order\": 47,\n" +
                "        \"profile_path\": \"/4BMub3CcZwJIgYu4LjzG0I804wM.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 161,\n" +
                "        \"character\": \"FN-0878\",\n" +
                "        \"credit_id\": \"5dfaea2bd1a8930019823d2f\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 100195,\n" +
                "        \"name\": \"Dhani Harrison\",\n" +
                "        \"order\": 48,\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 162,\n" +
                "        \"character\": \"Rey's Mother\",\n" +
                "        \"credit_id\": \"5dfaea48d1a893001481dcc3\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 1388593,\n" +
                "        \"name\": \"Jodie Comer\",\n" +
                "        \"order\": 49,\n" +
                "        \"profile_path\": \"/dqLaCHIygHuHJRt8PNSPhcDzUDd.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 163,\n" +
                "        \"character\": \"\",\n" +
                "        \"credit_id\": \"5dfaea5b65686e00138db0c8\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1371049,\n" +
                "        \"name\": \"Billy Howle\",\n" +
                "        \"order\": 50,\n" +
                "        \"profile_path\": \"/oThU8PRDz0YvsINvN6YD2qAoKy3.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 164,\n" +
                "        \"character\": \"Adi Gallia (voice)\",\n" +
                "        \"credit_id\": \"5dfb26b35ed9620016e452bc\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1213639,\n" +
                "        \"name\": \"Angelique Perrin\",\n" +
                "        \"order\": 51,\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"cast_id\": 165,\n" +
                "        \"character\": \"Klaud\",\n" +
                "        \"credit_id\": \"5dfb2f9c65686e00188dabb5\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1233264,\n" +
                "        \"name\": \"Nick Kellington\",\n" +
                "        \"order\": 52,\n" +
                "        \"profile_path\": null\n" +
                "      }\n" +
                "    ],\n" +
                "    \"crew\": [\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc330e4f5801000ff72c57\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1,\n" +
                "        \"job\": \"Characters\",\n" +
                "        \"name\": \"George Lucas\",\n" +
                "        \"profile_path\": \"/8qxin8urtFE0NqaZNFWOuV537bH.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc3422cdf2e600133b01f7\",\n" +
                "        \"department\": \"Sound\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 491,\n" +
                "        \"job\": \"Original Music Composer\",\n" +
                "        \"name\": \"John Williams\",\n" +
                "        \"profile_path\": \"/2Ats98PB1SH2yfEPikiLdhRuXZm.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc336f4284ea0014f8ea50\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 489,\n" +
                "        \"job\": \"Producer\",\n" +
                "        \"name\": \"Kathleen Kennedy\",\n" +
                "        \"profile_path\": \"/ndgYlie0PHkyqEiEBGM8SqrPOkr.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc34c74f58010016f736d3\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 496,\n" +
                "        \"job\": \"Production Design\",\n" +
                "        \"name\": \"Rick Carter\",\n" +
                "        \"profile_path\": \"/nIkkWGDlvAuZUAmVQZOIKew9kxw.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36f23faba0001300f542\",\n" +
                "        \"department\": \"Costume & Make-Up\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 605,\n" +
                "        \"job\": \"Costume Design\",\n" +
                "        \"name\": \"Michael Kaplan\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc34a24f58010016f73691\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 6052,\n" +
                "        \"job\": \"Casting\",\n" +
                "        \"name\": \"April Webster\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36da0cd44600142c7e78\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 13588,\n" +
                "        \"job\": \"Set Decoration\",\n" +
                "        \"name\": \"Rosemary Brandenburg\",\n" +
                "        \"profile_path\": \"/rrlIjwGSprN47jjXCVIhoIBjKXr.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc32744f58010016f733af\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15344,\n" +
                "        \"job\": \"Screenplay\",\n" +
                "        \"name\": \"J.J. Abrams\",\n" +
                "        \"profile_path\": \"/h0o12c399M5hTKWl0qdgwhaddNt.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc377e3faba0001500ea89\",\n" +
                "        \"department\": \"Directing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15344,\n" +
                "        \"job\": \"Director\",\n" +
                "        \"name\": \"J.J. Abrams\",\n" +
                "        \"profile_path\": \"/h0o12c399M5hTKWl0qdgwhaddNt.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc335fcdf2e600133affda\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15344,\n" +
                "        \"job\": \"Producer\",\n" +
                "        \"name\": \"J.J. Abrams\",\n" +
                "        \"profile_path\": \"/h0o12c399M5hTKWl0qdgwhaddNt.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc328eaad9c20010c2cc8c\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15344,\n" +
                "        \"job\": \"Story\",\n" +
                "        \"name\": \"J.J. Abrams\",\n" +
                "        \"profile_path\": \"/h0o12c399M5hTKWl0qdgwhaddNt.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc3439aad9c20018c2c597\",\n" +
                "        \"department\": \"Camera\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 15348,\n" +
                "        \"job\": \"Director of Photography\",\n" +
                "        \"name\": \"Dan Mindel\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc344dcdf2e600133b0263\",\n" +
                "        \"department\": \"Editing\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 15349,\n" +
                "        \"job\": \"Editor\",\n" +
                "        \"name\": \"Maryann Brandon\",\n" +
                "        \"profile_path\": \"/iO29og3UgVa9Sn4jZG1KpcTBq0a.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc34934f58010012f738b8\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 1,\n" +
                "        \"id\": 16363,\n" +
                "        \"job\": \"Casting\",\n" +
                "        \"name\": \"Nina Gold\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc33c63faba0001900f81e\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 19770,\n" +
                "        \"job\": \"Executive Producer\",\n" +
                "        \"name\": \"Callum Greene\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36064284ea0011f8bd9e\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 30463,\n" +
                "        \"job\": \"Supervising Art Director\",\n" +
                "        \"name\": \"Paul Inglis\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc3280aad9c20012c2db85\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 131680,\n" +
                "        \"job\": \"Screenplay\",\n" +
                "        \"name\": \"Chris Terrio\",\n" +
                "        \"profile_path\": \"/oljJKka3NTgiQhqhDs1SaCtcWMW.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc32a24f58010012f736a4\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 131680,\n" +
                "        \"job\": \"Story\",\n" +
                "        \"name\": \"Chris Terrio\",\n" +
                "        \"profile_path\": \"/oljJKka3NTgiQhqhDs1SaCtcWMW.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc34af3faba0001300f16f\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 765144,\n" +
                "        \"job\": \"Casting\",\n" +
                "        \"name\": \"Alyssa Weisberg\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc32d8a80673001411ecc7\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 930707,\n" +
                "        \"job\": \"Story\",\n" +
                "        \"name\": \"Colin Trevorrow\",\n" +
                "        \"profile_path\": \"/i0HBClPMPKn3aq8QeQqyThS67EK.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc3654a80673001411f21b\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 983309,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Mike Stallion\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc32e7a80673001411eced\",\n" +
                "        \"department\": \"Writing\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1020013,\n" +
                "        \"job\": \"Story\",\n" +
                "        \"name\": \"Derek Connolly\",\n" +
                "        \"profile_path\": \"/k1VQLCP4RAAkuXZ5HWrdL1NQnWR.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36704f58010016f738fc\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1073372,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Oli van der Vijver\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc33dd0cd44600122c9927\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1102139,\n" +
                "        \"job\": \"Executive Producer\",\n" +
                "        \"name\": \"Jason D. McGatlin\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36c7cdf2e600113b021e\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1299326,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Matt Wynne\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc35803faba0001300f27f\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1388848,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Jim Barr\",\n" +
                "        \"profile_path\": \"/myLNBZXXESp1YzDkBoKQX8g4Yup.jpg\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc361b3faba0001900fb6b\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1466745,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Samy Keilani\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc34dc4284ea0017f8c319\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1546026,\n" +
                "        \"job\": \"Production Design\",\n" +
                "        \"name\": \"Kevin Jenkins\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc3631cdf2e600133b055d\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1546027,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Ashley Lamont\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc35a6a80673001211a37b\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1551809,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Lydia Fry\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc35bb4284ea0014f8ed83\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1551810,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Liam Georgensen\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc345b0cd44600192ca6a1\",\n" +
                "        \"department\": \"Editing\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1636658,\n" +
                "        \"job\": \"Editor\",\n" +
                "        \"name\": \"Stefan Grube\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc340e3faba0001300f0c1\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1684988,\n" +
                "        \"job\": \"Associate Producer\",\n" +
                "        \"name\": \"Nour Dardari\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc337fa80673001411eda7\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1729808,\n" +
                "        \"job\": \"Producer\",\n" +
                "        \"name\": \"Michelle Rejwan\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc33a70cd44600122c98cc\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"gender\": 2,\n" +
                "        \"id\": 1750922,\n" +
                "        \"job\": \"Executive Producer\",\n" +
                "        \"name\": \"Tommy Gormley\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc3642cdf2e600133b0578\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1860041,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Oliver Roberts\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc35e1a80673001211a3d7\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 1915704,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Patrick Harris\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc35954284ea0014f8ed33\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 2000346,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Claire Fleming\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36b0a80673001411f25e\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 2157661,\n" +
                "        \"job\": \"Construction Foreman\",\n" +
                "        \"name\": \"Robert Voysey\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc35f33faba0001300f374\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 2286355,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"name\": \"Helena Holmes\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5ddc36954284ea0014f8ef78\",\n" +
                "        \"department\": \"Crew\",\n" +
                "        \"gender\": 0,\n" +
                "        \"id\": 2467589,\n" +
                "        \"job\": \"Visual Effects Art Director\",\n" +
                "        \"name\": \"Chris Voy\",\n" +
                "        \"profile_path\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"videos\": {\n" +
                "    \"results\": [\n" +
                "      {\n" +
                "        \"id\": \"5cb0c7fdc3a3683c26ac7167\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"adzYW5DZoWs\",\n" +
                "        \"name\": \"Official Teaser\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Teaser\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5d63d8a56dea3a09459360ec\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"3n1T3HxHd7Y\",\n" +
                "        \"name\": \"D23 Special Look\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Teaser\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dae73fd1cac8c001b0b489f\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"8Qn_spdM5Zg\",\n" +
                "        \"name\": \"Final Trailer\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Trailer\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dd9741c3faba00013fc2f85\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"1qphC39PJf4\",\n" +
                "        \"name\": \"“Fate” TV Spot\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Teaser\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dd9745228723c001457c66a\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"Zsp8iOt76YU\",\n" +
                "        \"name\": \"“End” TV Spot\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Teaser\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5ddb8cb1a80673001710e80a\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"mml9IGoDia8\",\n" +
                "        \"name\": \"Special Look\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Featurette\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5ddc299c4284ea0011f8acb3\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"BCPiBWrIaSI\",\n" +
                "        \"name\": \"Star Wars: The Rise of Skywalker | Film Clip\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Clip\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5de1041f0cd446001433cf25\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"CJhKQeblpYk\",\n" +
                "        \"name\": \"Star Wars: The Rise Of Skywalker | Featurette\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Featurette\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5de1045e0cd4460012347da8\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"bjdjyvnOGtg\",\n" +
                "        \"name\": \"Star Wars: The Rise of Skywalker | “Duel” TV Spot\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Teaser\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dece8cd6d000c0014564780\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"VbVu1eFiseA\",\n" +
                "        \"name\": \"Star Wars: The Rise of Skywalker | Friendship Featurette\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Featurette\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dece9b685da12001a48934e\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"HUAUvx1_WDE\",\n" +
                "        \"name\": \"Star Wars: Episode 9 - The Rise of Skywalker - On Set Exclusive | Vanity Fair\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Behind the Scenes\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dfa74bed1a893001280c07d\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"rlTJRiGIn-4\",\n" +
                "        \"name\": \"Star Wars: The Rise Of Skywalker Amazon Special Look\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Featurette\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"reviews\": {\n" +
                "    \"page\": 1,\n" +
                "    \"results\": [],\n" +
                "    \"total_pages\": 0,\n" +
                "    \"total_results\": 0\n" +
                "  }\n" +
                "}";
    }

    public void injectSeriesDetailsString() {
        UpdateMediaWorker.sSeriesDetailsStringInject = "{\n" +
                "  \"backdrop_path\": \"/weeee!!!\",\n" +
                "  \"created_by\": [\n" +
                "    {\n" +
                "      \"id\": 15277,\n" +
                "      \"credit_id\": \"5bb6f5c39251410dc601d77f\",\n" +
                "      \"name\": \"Jon Favreau\",\n" +
                "      \"gender\": 2,\n" +
                "      \"profile_path\": \"/rOVBKURoR7TrG8MYxTuNUFj3E68.jpg\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"episode_run_time\": [\n" +
                "    35\n" +
                "  ],\n" +
                "  \"first_air_date\": \"2028-12-18\",\n" +
                "  \"genres\": [\n" +
                "    {\n" +
                "      \"id\": 10765,\n" +
                "      \"name\": \"Action\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 10759,\n" +
                "      \"name\": \"Adventure\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"homepage\": \"https://disneyplusoriginals.disney.com/show/the-mandalorian\",\n" +
                "  \"id\": 82856,\n" +
                "  \"in_production\": true,\n" +
                "  \"languages\": [\n" +
                "    \"en\"\n" +
                "  ],\n" +
                "  \"last_air_date\": \"2019-12-18\",\n" +
                "  \"last_episode_to_air\": {\n" +
                "    \"air_date\": \"2019-12-18\",\n" +
                "    \"episode_number\": 7,\n" +
                "    \"id\": 1987337,\n" +
                "    \"name\": \"Chapter 7: The Reckoning\",\n" +
                "    \"overview\": \"An old rival extends an invitation for The Mandalorian to make peace.\",\n" +
                "    \"production_code\": \"\",\n" +
                "    \"season_number\": 1,\n" +
                "    \"show_id\": 82856,\n" +
                "    \"still_path\": \"/m483Gihn8vAt0cW5RXe5YRtchIQ.jpg\",\n" +
                "    \"vote_average\": 9,\n" +
                "    \"vote_count\": 3\n" +
                "  },\n" +
                "  \"name\": \"The Mandalorian\",\n" +
                "  \"next_episode_to_air\": {\n" +
                "    \"air_date\": \"2019-12-27\",\n" +
                "    \"episode_number\": 8,\n" +
                "    \"id\": 2023593,\n" +
                "    \"name\": \"Episode 8\",\n" +
                "    \"overview\": \"overview\",\n" +
                "    \"production_code\": \"\",\n" +
                "    \"season_number\": 1,\n" +
                "    \"show_id\": 82856,\n" +
                "    \"still_path\": null,\n" +
                "    \"vote_average\": 0,\n" +
                "    \"vote_count\": 0\n" +
                "  },\n" +
                "  \"networks\": [\n" +
                "    {\n" +
                "      \"name\": \"Disney+\",\n" +
                "      \"id\": 2739,\n" +
                "      \"logo_path\": \"/gJ8VX6JSu3ciXHuC2dDGAo2lvwM.png\",\n" +
                "      \"origin_country\": \"US\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"number_of_episodes\": 8,\n" +
                "  \"number_of_seasons\": 1,\n" +
                "  \"origin_country\": [\n" +
                "    \"US\",\"UK\"\n" +
                "  ],\n" +
                "  \"original_language\": \"en\",\n" +
                "  \"original_name\": \"The Mandalorian\",\n" +
                "  \"overview\": \"overview\",\n" +
                "  \"popularity\": 5000,\n" +
                "  \"poster_path\": \"/nawnaw\",\n" +
                "  \"production_companies\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"logo_path\": \"/o86DbpburjxrqAzEDhXZcyE8pDb.png\",\n" +
                "      \"name\": \"Lucasfilm\",\n" +
                "      \"origin_country\": \"US\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 109755,\n" +
                "      \"logo_path\": \"/hUCbTgfDPp1sIo8BOI0AaOMx1Si.png\",\n" +
                "      \"name\": \"Walt Disney Studios\",\n" +
                "      \"origin_country\": \"US\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"seasons\": [\n" +
                "    {\n" +
                "      \"air_date\": \"2019-11-12\",\n" +
                "      \"episode_count\": 8,\n" +
                "      \"id\": 110346,\n" +
                "      \"name\": \"Season 1\",\n" +
                "      \"overview\": \"\",\n" +
                "      \"poster_path\": \"/nawnaw\",\n" +
                "      \"season_number\": 1\n" +
                "    }\n" +
                "  ],\n" +
                "  \"status\": \"Returning Series\",\n" +
                "  \"type\": \"Scripted\",\n" +
                "  \"vote_average\": 6.7,\n" +
                "  \"vote_count\": 315,\n" +
                "  \"credits\": {\n" +
                "    \"cast\": [\n" +
                "      {\n" +
                "        \"character\": \"The Mandalorian\",\n" +
                "        \"credit_id\": \"5beb247f92514143e6058194\",\n" +
                "        \"id\": 1253360,\n" +
                "        \"name\": \"Pedro Pascal\",\n" +
                "        \"gender\": 2,\n" +
                "        \"profile_path\": \"/wAkkWX9J4n1MsLGxJxXSPvjWuzY.jpg\",\n" +
                "        \"order\": 0\n" +
                "      }\n" +
                "    ],\n" +
                "    \"crew\": [\n" +
                "      {\n" +
                "        \"credit_id\": \"5c43165c0e0a26676d1a7280\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"id\": 1826954,\n" +
                "        \"name\": \"Page Rosenberg-Marvin\",\n" +
                "        \"gender\": 1,\n" +
                "        \"job\": \"Production Supervisor\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5de37ee3c51acd0011f9b459\",\n" +
                "        \"department\": \"Production\",\n" +
                "        \"id\": 1760516,\n" +
                "        \"name\": \"John Bartnicki\",\n" +
                "        \"gender\": 2,\n" +
                "        \"job\": \"Co-Producer\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5de37fdd3faba000130c1119\",\n" +
                "        \"department\": \"Art\",\n" +
                "        \"id\": 1271755,\n" +
                "        \"name\": \"John Lord Booth III\",\n" +
                "        \"gender\": 0,\n" +
                "        \"job\": \"Art Direction\",\n" +
                "        \"profile_path\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"credit_id\": \"5de380023faba000150d152c\",\n" +
                "        \"department\": \"Costume & Make-Up\",\n" +
                "        \"id\": 1059586,\n" +
                "        \"name\": \"Carlton Coleman\",\n" +
                "        \"gender\": 0,\n" +
                "        \"job\": \"Makeup Artist\",\n" +
                "        \"profile_path\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"videos\": {\n" +
                "    \"results\": [\n" +
                "      {\n" +
                "        \"id\": \"5d609d5aeec4f30015add686\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"aOC8E8z_ifw\",\n" +
                "        \"name\": \"The Mandalorian | Official Trailer | Disney+ | Streaming Nov. 12\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 360,\n" +
                "        \"type\": \"Trailer\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5db79fff2d37210015e82187\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"XmI7WKrAtqs\",\n" +
                "        \"name\": \"The Mandalorian – Official Trailer 2 | Disney+ | Streaming Nov. 12\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Trailer\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dd8f09f12aabc0011906541\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"2RVnrBLOBcI\",\n" +
                "        \"name\": \"The  | Special Look | Disney+\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Featurette\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"5dd8f0dc28723c18ad556b3e\",\n" +
                "        \"iso_639_1\": \"en\",\n" +
                "        \"iso_3166_1\": \"US\",\n" +
                "        \"key\": \"o3CUM-iFEFk\",\n" +
                "        \"name\": \"The Mandalorian | Exclusive Clip | Disney+\",\n" +
                "        \"site\": \"YouTube\",\n" +
                "        \"size\": 1080,\n" +
                "        \"type\": \"Clip\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"reviews\": {\n" +
                "    \"page\": 1,\n" +
                "    \"results\": [],\n" +
                "    \"total_pages\": 0,\n" +
                "    \"total_results\": 0\n" +
                "  }\n" +
                "}";
    }

    //test movie update without notifier
    @Test
    public void testMovieUpdate() {
        injectMovieDetailsString();

        //create media data
        MovieData movieData = new MovieData("181812", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_WATCHING);
        movieData.setUserRating(8);
        movieData.setReleaseStatus(ApiConstants.RELEASE_STATUS_IN_PRODUCTION);
        movieData.setTraktId("55555");

        mDb.movieDataDao().addMovieData(movieData);

        Data inputData = new Data.Builder()
                .putLong("SLEEP_DURATION", 10_000L)
                .build();

        UpdateMediaWorker worker =
                (UpdateMediaWorker) TestWorkerBuilder.from(context,
                        UpdateMediaWorker.class,
                        mExecutor)
                        .setInputData(inputData)
                        .build();

        ListenableWorker.Result result = worker.doWork();
        assertThat(result, is(Worker.Result.success()));

        movieData = mDb.movieDataDao().getMovieByIdAlt(movieData.getId());

        if (!movieData.getVoteAverage().equals("6.7")) fail();
        if (!movieData.getTitle().equals("Star Wars: The Rise of Skywalker")) fail();
        if (movieData.getPopularity() == 5000) fail();
        if (!movieData.getPosterPath().equals(ApiConstants.IMAGE_BASE_URL + ApiConstants.POSTER_IMAGE_SIZE + "/nawnaw")) fail();
        if (!movieData.getOriginalLanguage().equals("en")) fail();
        if (!movieData.getOriginalTitle().equals("Star Wars: The Rise of Skywalker")) fail();

        ArrayList<String> genres = new ArrayList<>();
        genres.add("Action");
        genres.add("Adventure");

        if (!movieData.getGenres().get(0).equals(genres.get(0))) fail();
        if (!movieData.getGenres().get(1).equals(genres.get(1))) fail();
        if (!movieData.getBackdropPath().equals(ApiConstants.IMAGE_BASE_URL + ApiConstants.BACKDROP_IMAGE_SIZE + "/weeee!!!")) fail();
        if (!movieData.isAdult()) fail();
        if (!movieData.getOverview().equals("overview")) fail();
        if (!movieData.getReleaseDate().equals("2028-12-18")) fail();

        //ensure watch status is preserved
        if (movieData.getWatchStatus() != MediaData.WATCH_STATUS_WATCHING) fail();
        //ensure user rating is preserved
        if (movieData.getUserRating() != 8.) fail();
        //ensure release status is preserved
        if (!movieData.getReleaseStatus().equals(MovieApiConstants.RELEASE_STATUS_RELEASED)) fail();
        //ensure trakt id is preserved
        if (!movieData.getTraktId().equals("55555")) fail();
    }

    //test series update without notifier
    @Test
    public void testSeriesUpdate() {
        injectSeriesDetailsString();

        //create media data
        SeriesData seriesData = new SeriesData("82856", "", "", "",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setWatchStatus(MediaData.WATCH_STATUS_WATCHING);
        seriesData.setUserRating(8);
        seriesData.setReleaseStatus(ApiConstants.RELEASE_STATUS_IN_PRODUCTION);
        seriesData.setTraktId("55555");

        mDb.seriesDataDao().addSeriesData(seriesData);

        Data inputData = new Data.Builder()
                .putLong("SLEEP_DURATION", 10_000L)
                .build();

        UpdateMediaWorker worker =
                (UpdateMediaWorker) TestWorkerBuilder.from(context,
                        UpdateMediaWorker.class,
                        mExecutor)
                        .setInputData(inputData)
                        .build();

        ListenableWorker.Result result = worker.doWork();
        assertThat(result, is(Worker.Result.success()));

        seriesData = mDb.seriesDataDao().getSeriesByIdAlt(seriesData.getId());

        if (!seriesData.getVoteAverage().equals("6.7")) fail();
        if (!seriesData.getTitle().equals("The Mandalorian")) fail();
        if (seriesData.getPopularity() == 5000) fail();
        if (!seriesData.getPosterPath().equals(ApiConstants.IMAGE_BASE_URL + ApiConstants.POSTER_IMAGE_SIZE + "/nawnaw")) fail();
        if (!seriesData.getOriginalLanguage().equals("en")) fail();
        if (!seriesData.getOriginalTitle().equals("The Mandalorian")) fail();

        ArrayList<String> originCountries = new ArrayList<>();
        originCountries.add("US");
        originCountries.add("UK");

        if (!seriesData.getCountryOfOrigin().get(0).equals(originCountries.get(0))) fail();
        if (!seriesData.getCountryOfOrigin().get(1).equals(originCountries.get(1))) fail();

        ArrayList<String> genres = new ArrayList<>();
        genres.add("Action");
        genres.add("Adventure");

        if (!seriesData.getGenres().get(0).equals(genres.get(0))) fail();
        if (!seriesData.getGenres().get(1).equals(genres.get(1))) fail();
        if (!seriesData.getBackdropPath().equals(ApiConstants.IMAGE_BASE_URL + ApiConstants.BACKDROP_IMAGE_SIZE + "/weeee!!!")) fail();
        if (!seriesData.getOverview().equals("overview")) fail();
        if (!seriesData.getReleaseDate().equals("2028-12-18")) fail();

        //ensure watch status is preserved
        if (seriesData.getWatchStatus() != MediaData.WATCH_STATUS_WATCHING) fail();
        //ensure user rating is preserved
        if (seriesData.getUserRating() != 8.) fail();
        //ensure release status is preserved
        if (!seriesData.getReleaseStatus().equals(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES)) fail();
        //ensure trakt id is preserved
        if (!seriesData.getTraktId().equals("55555")) fail();
    }
}
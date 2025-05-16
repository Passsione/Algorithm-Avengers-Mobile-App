package com.pbdvmobile.app.data.dao;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Prize;
import com.pbdvmobile.app.data.model.RedeemPrize;

import java.util.ArrayList;
import java.util.List;

public class PrizeDao {

    private final SqlOpenHelper dbHelper;


    // Prize section
    public PrizeDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertPrize(Prize prize) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_PRIZE_NAME, prize.getPrizeName());
        values.put(SqlOpenHelper.KEY_PRIZE_COST, prize.getCostInCredits());

        long id = db.insert(SqlOpenHelper.TABLE_PRIZES, null, values);
        db.close();
        return id;
    }

    public Prize getPrizeById(int prizeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_PRIZES,
                null,
                SqlOpenHelper.KEY_PRIZE_ID + "=?",
                new String[]{String.valueOf(prizeId)},
                null, null, null);

        Prize prize = null;
        if (cursor.moveToFirst()) {
            prize = new Prize();
            prize.setPrizeId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_PRIZE_ID)));
            prize.setPrizeName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_PRIZE_NAME)));
            prize.setCostInCredits(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_PRIZE_COST)));
            cursor.close();
        }
        db.close();
        return prize;
    }

    public List<Prize> getAllPrizes() {
        List<Prize> prizes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_PRIZES,
                null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Prize prize = new Prize();
                prize.setPrizeId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_PRIZE_ID)));
                prize.setPrizeName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_PRIZE_NAME)));
                prize.setCostInCredits(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_PRIZE_COST)));
                prizes.add(prize);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return prizes;
    }

    public int updatePrize(Prize prize) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_PRIZE_NAME, prize.getPrizeName());
        values.put(SqlOpenHelper.KEY_PRIZE_COST, prize.getCostInCredits());

        int rowsAffected = db.update(SqlOpenHelper.TABLE_PRIZES, values,
                SqlOpenHelper.KEY_PRIZE_ID + "=?",
                new String[]{String.valueOf(prize.getPrizeId())});
        db.close();
        return rowsAffected;
    }

    public int deletePrize(int prizeId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(SqlOpenHelper.TABLE_PRIZES,
                SqlOpenHelper.KEY_PRIZE_ID + "=?",
                new String[]{String.valueOf(prizeId)});
        db.close();
        return rowsAffected;
    }


    // Redeem Prize section
    public long insertRedeemPrize(RedeemPrize redeemPrize) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_REDEEM_STUDENT_NUM, redeemPrize.getStudentNum());
        values.put(SqlOpenHelper.KEY_REDEEM_PRIZE_ID, redeemPrize.getPrizeId());
        values.put(SqlOpenHelper.KEY_REDEEM_STATUS, redeemPrize.getStatus().name());

        long id = db.insert(SqlOpenHelper.TABLE_REDEEM_PRIZES, null, values);
        db.close();
        return id;
    }

    public RedeemPrize getRedeemPrizeById(int redeemId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_REDEEM_PRIZES,
                null,
                SqlOpenHelper.KEY_REDEEM_ID + "=?",
                new String[]{String.valueOf(redeemId)},
                null, null, null);

        RedeemPrize redeemPrize = null;
        if (cursor.moveToFirst()) {
            redeemPrize = cursorToRedeemPrize(cursor);
            cursor.close();
        }
        db.close();
        return redeemPrize;
    }

    public List<RedeemPrize> getRedeemPrizesByStudentNum(int studentNum) {
        List<RedeemPrize> redeemPrizes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_REDEEM_PRIZES,
                null,
                SqlOpenHelper.KEY_REDEEM_STUDENT_NUM + "=?",
                new String[]{String.valueOf(studentNum)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                RedeemPrize redeemPrize = cursorToRedeemPrize(cursor);
                redeemPrizes.add(redeemPrize);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return redeemPrizes;
    }

    public int updateRedeemPrizeStatus(int redeemId, RedeemPrize.Status status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_REDEEM_STATUS, status.name());

        int rowsAffected = db.update(SqlOpenHelper.TABLE_REDEEM_PRIZES, values,
                SqlOpenHelper.KEY_REDEEM_ID + "=?",
                new String[]{String.valueOf(redeemId)});
        db.close();
        return rowsAffected;
    }
    public int deleteRedeemPrize(int redeemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(SqlOpenHelper.TABLE_REDEEM_PRIZES,
                SqlOpenHelper.KEY_REDEEM_ID + "=?",
                new String[]{String.valueOf(redeemId)});
        db.close();
        return rowsAffected;
    }

    private RedeemPrize cursorToRedeemPrize(Cursor cursor) {
        RedeemPrize redeemPrize = new RedeemPrize();
        redeemPrize.setRedeemId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_REDEEM_ID)));
        redeemPrize.setStudentNum(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_REDEEM_STUDENT_NUM)));
        redeemPrize.setPrizeId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_REDEEM_PRIZE_ID)));

        String statusString = cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_REDEEM_STATUS));
        redeemPrize.setStatus(RedeemPrize.Status.valueOf(statusString));

        return redeemPrize;
    }
}
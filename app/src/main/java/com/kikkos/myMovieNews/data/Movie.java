package com.kikkos.myMovieNews.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.kikkos.myMovieNews.R;

/**
 * Created by kikkos on 8/11/2016.
 */
public class Movie implements Parcelable {

    int id;
    String poster_path;
    int poster;
    String title;
    double rating;
    String release_date;
    String overview;
    boolean favourite;

    public Movie() {

    }

    public Movie(Parcel parcel){
        this.id = parcel.readInt();
        this.poster_path = parcel.readString();
        this.poster = parcel.readInt();
        this.title = parcel.readString();
        this.rating = parcel.readDouble();
        this.release_date = parcel.readString();
        this.overview = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.poster_path);
        dest.writeInt(this.poster);
        dest.writeString(this.title);
        dest.writeDouble(this.rating);
        dest.writeString(this.release_date);
        dest.writeString(this.overview);
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>(){

        public Movie createFromParcel(Parcel parcel){
            return new Movie(parcel);
        }

        public Movie[] newArray(int i){
            return new Movie[i];
        }
    };

    public void setId(int id){
        this.id = id;
    }

    public void setPosterPath(String url){
        this.poster_path = url;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setRating(double rating){
        this.rating = rating;
    }

    public void setReleaseDate(String date){
        this.release_date = date;
    }

    public void setOverview(String overview){
        this.overview = overview;
    }

    public void setFavourite(boolean status){
        this.favourite = status;
    }

    public int getId(){
        return id;
    }

    public String getPosterPath(){
        return this.poster_path;
    }

    public String getPosterUrlString(Context context){
        return context.getString(R.string.poster_path_base_url) + this.poster_path;
    }

    public String getTitle(){
        return this.title;
    }

    public double getRating(){
        return this.rating;
    }

    public String getReleaseDate(){
        return this.release_date;
    }

    public String getOverview(){
        return this.overview;
    }

    public boolean getFavourite(){
        return this.favourite;
    }
}

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
    double popularity;
    String release_date;
    String overview;
    boolean favourite;
    boolean isPopular;
    boolean isTopRated;

    public Movie() {

    }

    public Movie(Parcel parcel){
        this.id = parcel.readInt();
        this.poster_path = parcel.readString();
        this.poster = parcel.readInt();
        this.title = parcel.readString();
        this.rating = parcel.readDouble();
        this.popularity = parcel.readDouble();
        this.release_date = parcel.readString();
        this.overview = parcel.readString();
        this.favourite = parcel.readByte() > 0;
        this.isPopular = parcel.readByte() > 0;
        this.isTopRated = parcel.readByte() > 0;
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
        dest.writeDouble(this.popularity);
        dest.writeString(this.release_date);
        dest.writeString(this.overview);
        dest.writeByte((byte)(this.favourite ? 1 : 0));
        dest.writeByte((byte)(this.isPopular ? 1 : 0));
        dest.writeByte((byte)(this.isTopRated ? 1 : 0));
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

    public void setPopularity(double popularity){
        this.popularity = popularity;
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

    public void setIsPopular(boolean status){
        this.isPopular = status;
    }

    public void setIsTopRated(boolean status){
        this.isTopRated = status;
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

    public double getPopularity(){
        return this.popularity;
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

    public boolean getIsPopular(){
        return this.isPopular;
    }

    public boolean getIsTopRated(){
        return this.isTopRated;
    }
}

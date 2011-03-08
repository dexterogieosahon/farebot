/*
 * DesfireCard.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.cepas;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.DesfireApplication;
import com.codebutler.farebot.mifare.DesfireFile;
import com.codebutler.farebot.mifare.DesfireFileSettings;
import com.codebutler.farebot.mifare.DesfireManufacturingData;
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.mifare.DesfireFile.InvalidDesfireFile;
import com.codebutler.farebot.mifare.DesfireFileSettings.RecordDesfireFileSettings;
import com.codebutler.farebot.mifare.DesfireFileSettings.StandardDesfireFileSettings;
import com.codebutler.farebot.mifare.MifareCard.CardType;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CEPASCard extends MifareCard
{
    private CEPASPurse[] mPurses;
    private CEPASHistory[] mHistories;

    public static CEPASCard dumpTag (byte[] tagId, Tag tag) throws Exception
    {
        List<DesfireApplication> apps = new ArrayList<DesfireApplication>();

        IsoDep tech = IsoDep.get(tag);

        try {
        	tech.connect();
        }
        catch (IOException ex) {
        	Log.e("FareBot", "Unable to connect: " + ex.toString());
        }

        CEPASPurse[] cepasPurses = new CEPASPurse[16];
        CEPASHistory[] cepasHistories = new CEPASHistory[16];
        
        try {
            CEPASProtocol cepasTag = new CEPASProtocol(tech);
            for(int purseId = 0; purseId < cepasPurses.length; purseId++)
            	cepasPurses[purseId] = cepasTag.getPurse(purseId);
            for(int historyId = 0; historyId < cepasHistories.length; historyId++)
            	cepasHistories[historyId] = cepasTag.getHistory(historyId);
        } finally {
            if (tech.isConnected())
                tech.close();
        }

        return new CEPASCard(tagId, cepasPurses, cepasHistories);
    }

    CEPASCard(byte[] tagId, CEPASPurse[] purses, CEPASHistory[] histories)
    {
        super(tagId);
        mPurses = purses;
        mHistories = histories;
    }

    @Override
    public CardType getCardType () {
        return CardType.CEPAS;
    }
    
    public CEPASPurse getPurse(int purse)
    {
    	return mPurses[purse];
    }
    
    public CEPASHistory getHistory(int purse)
    {
    	return mHistories[purse];
    }

    public static final Parcelable.Creator<CEPASCard> CREATOR = new Parcelable.Creator<CEPASCard>() {
        public CEPASCard createFromParcel(Parcel source) {
            int tagIdLength = source.readInt();
            byte[] tagId = new byte[tagIdLength];
            source.readByteArray(tagId);

            CEPASPurse[] purses = new CEPASPurse[source.readInt()];
            for(int i=0; i<purses.length; i++)
            	purses[i] = (CEPASPurse) source.readParcelable(CEPASPurse.class.getClassLoader()); 

            CEPASHistory[] histories = new CEPASHistory[source.readInt()];
            for(int i=0; i<histories.length; i++)
            	histories[i] = (CEPASHistory) source.readParcelable(CEPASHistory.class.getClassLoader()); 


            return new CEPASCard(tagId, purses, histories);
        }

        public CEPASCard[] newArray (int size) {
            return new CEPASCard[size];
        }
    };

    public void writeToParcel (Parcel parcel, int flags)
    {
        super.writeToParcel(parcel, flags);
        parcel.writeInt(mPurses.length);
        for(int i=0; i<mPurses.length; i++)
        	parcel.writeParcelable(mPurses[i], flags);
        parcel.writeInt(mHistories.length);
        for(int i=0; i<mHistories.length; i++)
        	parcel.writeParcelable(mHistories[i], flags);
    }
    
    public int describeContents ()
    {
        return 0;
    }

    public static CEPASCard fromXML (byte[] cardId, Element rootElement)
    {
        NodeList purseElements = ((Element) rootElement.getElementsByTagName("purses").item(0)).getElementsByTagName("purse");
        CEPASPurse[] purses = new CEPASPurse[purseElements.getLength()];
        for(int i=0; i<purseElements.getLength(); i++)
        	purses[i] = CEPASPurse.fromXML((Element)purseElements.item(i));

        NodeList historyElements = ((Element) rootElement.getElementsByTagName("histories").item(0)).getElementsByTagName("history");
        CEPASHistory[] histories = new CEPASHistory[historyElements.getLength()];
        for(int i=0; i<historyElements.getLength(); i++)
        	histories[i] = CEPASHistory.fromXML((Element)historyElements.item(i));
        
        
        return new CEPASCard(cardId, purses, histories);
    }

    public Element toXML() throws Exception
    {
        Element root = super.toXML();

        Document doc = root.getOwnerDocument();

        Element pursesElement = doc.createElement("purses");
        Element historiesElement = doc.createElement("histories");
        
        for (CEPASPurse purse : mPurses)
        	pursesElement.appendChild(purse.toXML(doc));
        root.appendChild(pursesElement);

        for (CEPASHistory history : mHistories)
        	historiesElement.appendChild(history.toXML(doc));
        root.appendChild(historiesElement);

        return root;
    }
}
package com.rcryptotool.wifichatElGamalECCsecyrity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;

import android.R.menu;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.colorcloud.wifichat.R;
import com.rcryptotool.wifichatElGamalECCsecyrity.WiFiDirectApp.PTPLog;



/**
 * chat fragment attached to main activity.
 */
@SuppressLint("UseValueOf")
public class ChatFragment extends ListFragment {
	private static final String TAG = "PTP_ChatFrag";
	
	WiFiDirectApp mApp = null; 
	private static MainActivity mActivity = null;
	
	private ArrayList<MessageRow> mMessageList = null;   // a list of chat msgs.
    private ArrayAdapter<MessageRow> mAdapter= null;
	static int timeThreshold = 60000;  

    
    private EllipticCurve ec;
    private CryptoSystem cs;
    Key sk;
    Key pk;
    Key othersPublicKey;   
    
    int communicationPoint = 0;
    boolean Alice = false;
    boolean Bob = false;
    String answerToExamine ;
    static String question;
    static String answer;
    static String nonce;
    static String timeStamp;
    static String encryption = "secp112r1";
    static boolean encryptionEnDis = true;
	/**
     * Static factory to create a fragment object from tab click.
     */
    public static ChatFragment newInstance(Activity activity, String groupOwnerAddr, String msg) {
    	ChatFragment f = new ChatFragment();
    	mActivity = (MainActivity)activity;
    	
        Bundle args = new Bundle();
        args.putString("groupOwnerAddr", groupOwnerAddr);
        args.putString("initMsg", msg);
        f.setArguments(args);
        Log.d(TAG, "newInstance :" + groupOwnerAddr + " : " + msg);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {  // this callback invoked after newInstance done.  
        super.onCreate(savedInstanceState);
        mApp = (WiFiDirectApp)mActivity.getApplication();
	    encryption = mApp.getEncryptionString();
	    encryptionEnDis = mApp.getEnableEncryption();
	    System.out.println ("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" + encryption);
	    try {
	    	if (encryption.compareTo("secp112r1")==0)
	    		ec = new EllipticCurve(new secp112r1());
	    	else if (encryption.compareTo("secp160r1")==0)
	    		ec = new EllipticCurve(new secp160r1());
	    	else
	    		ec = new EllipticCurve(new secp256r1());
		} catch (InsecureCurveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    cs = new ECCryptoSystem(ec);
	    sk = (ECKey)cs.generateKey();
	    pk = sk.getPublic();
	    Log.e(TAG, "ECC SUCCESSS");
        setRetainInstance(true);   // Tell the framework to try to keep this fragment around during a configuration change.
    }
    
    /**
     * the data you place in the Bundle here will be available in the Bundle given to onCreate(Bundle), etc.
     * only works when your activity is destroyed by android platform. If the user closed the activity, no call of this.
     * http://www.eigo.co.uk/Managing-State-in-an-Android-Activity.aspx
     */
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	outState.putParcelableArrayList("MSG_LIST", mMessageList);
    	Log.d(TAG, "onSaveInstanceState. " + mMessageList.get(0).mMsg);
    }
    
    /**
     * no matter your fragment is declared in main activity layout, or dynamically added thru fragment transaction
     * You need to inflate fragment view inside this function. 
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	// inflate the fragment's res layout. 
        View contentView = inflater.inflate(R.layout.chat_frag, container, false);  // no care whatever container is.
        final EditText inputEditText = (EditText)contentView.findViewById(R.id.edit_input);
        final Button sendBtn = (Button)contentView.findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send the chat text in current line to the server
				String inputMsg = inputEditText.getText().toString();
				inputEditText.setText("");
				InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);	
				String stringSend=inputMsg;
				if (encryptionEnDis == true) {
				////////////////////////////////////////////////////////////////
				///////// SEND !!!!!!     1 ALICE CREATE Pa , Con_Req
				/////////////////////////////////////////////////////////////////
				if (communicationPoint == 0 && Bob == false && Alice == false) {
					inputMsg = "FIRST MUST HAVE 3-WAY HSHAKE!";
					stringSend = createRequest_1 (pk);
				    Log.e(TAG,"Alice_1:"+stringSend);
				    communicationPoint+=1;
				    Alice = true;
				}
//				////////////////////////////////////////////////////////////////
//				//////// SEND!!!!!    1 BOB CREATE PERSONAL_QUEST , Pb , Con_Resp
				if (communicationPoint == 1 && Bob == true) {
					inputMsg = "Q:where are you?-A:ucy library";
					String[] get = createRequest_2 (inputMsg,pk);
					stringSend = get[0];
					answer = get[1];
					timeStamp=get[2];
					nonce= get[3];
					byte[] temp = null;
					try {
						System.out.println (othersPublicKey);
						temp = ChatFragment.callEncrypt(stringSend.getBytes("ISO-8859-1"), cs, othersPublicKey);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						stringSend= new String(temp,"ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    Log.e(TAG,"Alice_1:"+stringSend);
				    communicationPoint+=1;
				}				
				
//				
//				////////////////////////////////////////////////////////////////////
//				///////////2 SEND!!!!!!   ALICE SENDS Answer_Quest , Con_Resp
//				///////////////////////////////////////////////////////////////////
				else if (communicationPoint == 2 && Alice == true) {
					//String get = createRequest_3 (inputMsg,nonce,timeStamp);
					stringSend = "" + inputMsg + "-" + nonce + "-" + timeStamp ;
					System.out.println (stringSend);
					byte[] temp = null;
					try {
						temp = ChatFragment.callEncrypt(stringSend.getBytes("ISO-8859-1"), cs, othersPublicKey);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						stringSend= new String(temp,"ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    Log.e(TAG,"Alice_1:"+stringSend);
				    communicationPoint+=1;
				}			
//				
////				
////				///// REGULAR ENCRYPTION WITH THE OTHERS PUBLIC KEY
				else if (communicationPoint == 3) {
					stringSend = inputMsg;
					byte[] temp = null;
					try {
						temp = ChatFragment.callEncrypt(stringSend.getBytes("ISO-8859-1"), cs, othersPublicKey);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						stringSend= new String(temp, "ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    //Log.e(TAG,"Alice_1:"+stringSend);
				}			
//				
				
				}
				
				MessageRow row_show = new MessageRow(mApp.mDeviceName, inputMsg, null);
				MessageRow row_send = new MessageRow(mApp.mDeviceName, stringSend, null);
				
				
				try {
					appendChatMessage(row_show,false);
				} catch (NotOnMotherException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String jsonMsg = mApp.shiftInsertMessage(row_send);
				PTPLog.d(TAG, "sendButton clicked: sendOut data : " + jsonMsg);
				mActivity.pushOutMessage(jsonMsg);
			}

        });
        
        String groupOwnerAddr = getArguments().getString("groupOwnerAddr");
        String msg = getArguments().getString("initMsg");
        PTPLog.d(TAG, "onCreateView : fragment view created: msg :" + msg);
        
    	if( savedInstanceState != null ){
            mMessageList = savedInstanceState.getParcelableArrayList("MSG_LIST");
            Log.d(TAG, "onCreate : savedInstanceState: " + mMessageList.get(0).mMsg);
        }else if( mMessageList == null ){
        	// no need to setContentView, just setListAdapter, but listview must be android:id="@android:id/list"
            mMessageList = new ArrayList<MessageRow>();
            jsonArrayToList(mApp.mMessageArray, mMessageList);
            Log.d(TAG, "onCreate : jsonArrayToList : " + mMessageList.size() );
        }else {
        	Log.d(TAG, "onCreate : setRetainInstance good : ");
        }
        
        mAdapter = new ChatMessageAdapter(mActivity, mMessageList);
        
        setListAdapter(mAdapter);  // list fragment data adapter 
        
        PTPLog.d(TAG, "onCreate chat msg fragment: devicename : " + mApp.mDeviceName + " : " + getArguments().getString("initMsg"));
        return contentView;
    }
    
    @Override 
    public void onDestroyView(){ 
    	super.onDestroyView(); 
    	Log.d(TAG, "onDestroyView: ");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {  // invoked after fragment view created.
        super.onActivityCreated(savedInstanceState);
        
        if( mMessageList.size() > 0){
        	getListView().smoothScrollToPosition(mMessageList.size()-1);
        }
        
        setHasOptionsMenu(true);
        Log.d(TAG, "onActivityCreated: chat fragment displayed ");
    }
    
    /**
     * add a chat message to the list, return the format the message as " sender_addr : msg "
     * @throws NotOnMotherException 
     * @throws UnsupportedEncodingException 
     */
    @SuppressLint("UseValueOf")
	public void appendChatMessage(MessageRow row, boolean encrypt) throws NotOnMotherException, UnsupportedEncodingException {
    	Log.d(TAG, "appendChatMessage: chat fragment append msg: " + row.mSender + " ; " + row.mMsg);
    	
    	MessageRow row_show = row;	
    	if (encryptionEnDis == true ) {
			if (encrypt == true ){
				if (communicationPoint == 0 && Bob == false && Alice == false) {
					Bob=true;
					answerToExamine = row.mMsg;
				    String[] AlicePublic = retrieveAlicePublic(row.mMsg);
				    othersPublicKey= ((ECKey)pk).createOtherPartyPublicKey(AlicePublic[0],AlicePublic[1]);
				    System.out.println (othersPublicKey);
				    String time_1_Alice = AlicePublic[2];
				    String nonce_1_Alice = AlicePublic[3];			
					row_show.mMsg = new String ("Alice wants to connect!! Ask her something that only she can answer!");
					communicationPoint+=1;
				}
				if (communicationPoint == 1 && Alice == true) {
					answerToExamine = row.mMsg;
					byte []temp = ChatFragment.callDecrypt( answerToExamine.getBytes("ISO-8859-1"), cs, sk);
					String[] retr = retrieveBobPublic (new String(temp, "ISO-8859-1"));
					othersPublicKey = ((ECKey)pk).createOtherPartyPublicKey(retr[1],retr[2]);
					if (checkTimePassed(new Long(retr[3]).longValue(),new Long(timeStamp).longValue())) {
						timeStamp=retr[3];
						nonce=retr[4];
						row_show.mMsg = retr[0];
					}
					else {
						String	dummy = retr[10000];
					}
					communicationPoint+=1;
				}
				else if (communicationPoint == 2 && Bob == true) {
					answerToExamine = row.mMsg;
					byte []temp = ChatFragment.callDecrypt(answerToExamine.getBytes("ISO-8859-1"), cs, sk);
					System.out.println (new String(temp,"ISO-8859-1"));
					String[] retr = getTheAnswer (new String(temp,"ISO-8859-1"));
					row_show.mMsg = retr[0];
					String dummy;
					if (retr[0].equals(answer) && checkTimePassedAndNonce(retr[1],timeStamp,retr[2],nonce)) {
						answer=retr[0];
						timeStamp=retr[1];
						nonce=retr[2];
						System.out.println ("THE END!!!") ;
					}	
					else
						dummy = retr[10000];
					communicationPoint+=1;
				}
		
				else if (communicationPoint == 3) {
					byte []temp = ChatFragment.callDecrypt(row.mMsg.getBytes("ISO-8859-1"), cs, sk);		
					row_show.mMsg=new String(temp, "ISO-8859-1");
				}		
			}
    	}
    	
    	mMessageList.add(row_show);
    	getListView().smoothScrollToPosition(mMessageList.size()-1);
    	mAdapter.notifyDataSetChanged();  // notify the attached observer and views to refresh.
    	return;
    }
    


	static public boolean checkTimePassed (long timeGot, long l) {
		return ((l - timeGot) < timeThreshold ) ? true : false;
	}
	
	static public boolean checkTimePassedAndNonce (String timeGot, String l, String nonceGot, String n) {
		System.out.println (timeGot);
		System.out.println (l);
		System.out.println (nonceGot);
		System.out.println (n);
		//return (l.compareTo(timeGot)==0 &&   nonceGot.compareTo(n) == 0  ) ? true : false;
		return true;
	}
	
	static public String[] getTheAnswer (String ans) {
		return ans.split("-");
	}
	
	
	static public String createRequest_3 (String answer, String nonceBob, String timeBobSent) {
		return new String ("" + answer + "-" + timeBobSent + "-" + nonceBob ); 
	}
	
	
	static public String[] retrieveAlicePublic (String get) {
			String[] retr = get.split("-");
			return retr;
	}
	
	static public String[] retrieveBobPublic (String get) {
		String[] retr = get.split("-");
		return retr;
}	
	
	static public String createRequest_1 (Key pk) {
		  Random randomGen = new Random ();
		  String[] temp = pk.toString().split(":")[1].split(",");	    
		  String pk_1_Alice = temp[0].replace("|", "");
		  String pk_2_Alice = temp[1].replace(" ", "").replace("|"+encryption, "");
		  timeStamp = new String ("" + System.currentTimeMillis());
		  nonce = new String ("" + randomGen.nextInt(Integer.MAX_VALUE) );
		  String conRequestAlice_1 = new String (""+pk_1_Alice+"-"+pk_2_Alice+"-"+ timeStamp+"-"+ nonce);
		  return conRequestAlice_1;
	}

	static public String[] createRequest_2 (String bobq,Key pkBob) {
		  Random randomGen = new Random ();
			String[] parts = bobq.split("-");
			String[] splitQuestion = parts[0].split(":");
			String[] splitAnswer = parts[1].split(":");	    
			String question = splitQuestion[1];
			String answer = splitAnswer[1];     
			String[] tempBob = pkBob.toString().split(":")[1].split(",");	    
			String pk_1_Bob = tempBob[0].replace("|", "");
			String pk_2_Bob = tempBob[1].replace(" ", "").replace("|"+encryption, "");
			String timeStamp = new String ("" + System.currentTimeMillis());
			String nonce = new String ("" + randomGen.nextInt(Integer.MAX_VALUE));
			String conRequestBob_1 = new String (""+question+"-"+pk_1_Bob+"-"+pk_2_Bob+"-"+timeStamp+"-"+nonce);
			String[] returnValues = new String[4];
			returnValues[0]=conRequestBob_1;
			returnValues[1]=answer;
			returnValues[2]=timeStamp;
			returnValues[3]=nonce;
			return returnValues;
	}
	
	
	
	
	static byte[] callEncrypt (byte[]toen, CryptoSystem csBob ,Key pkBob ) throws UnsupportedEncodingException {
	
	    byte[] t=toen;
	    int now = 0;
	    int until=t.length;
	    byte[] transform= new byte[csBob.blockSize()];
	    int allocate = (until / 20 ) * csBob.returnPCS(pkBob) +until;
	    int where = 0;
	    if (until %20 != 0)
	    	allocate +=  csBob.returnPCS(pkBob)  ;
	    byte[]total= new byte[allocate];
	    while (true) {
	    	if (now + 20  < until) {
	    		System.arraycopy(t, now, transform, 0, csBob.blockSize());
	    		byte[] get=csBob.encrypt(transform, transform.length, pkBob);
	    		System.arraycopy(get, 0, total, where, get.length);
	    		now+= csBob.blockSize();
	    		where += get.length;
	    	}
	    	else {
	    		System.arraycopy(t, now, transform, 0, until-now);
	    		byte[] get=csBob.encrypt(transform,until-now, pkBob);
	    		System.arraycopy(get, 0, total, where, get.length);
	    		break;
	    	}
	    }	
		return total;
		
	}


	static byte[] callDecrypt (byte[]tode, CryptoSystem csBob ,Key skBob ) throws UnsupportedEncodingException {
	
	    byte[] t=tode;
	    int now = 0;
	    int until=t.length;
	    byte[] transform= new byte[csBob.blockSize()+ csBob.returnPCS(skBob) ];
	    int allocate = (until /	(csBob.blockSize()+csBob.returnPCS(skBob))) * csBob.blockSize();
	    
	    if (until %(csBob.blockSize()+csBob.returnPCS(skBob)) != 0)
	    	allocate += until - ((until / (csBob.blockSize()+csBob.returnPCS(skBob))) * (csBob.blockSize()+csBob.returnPCS(skBob))) - csBob.returnPCS(skBob)  ;
	    
	    byte[]total= new byte[allocate];
	    int where1 = 0;
	    while (true) {
	    	if (now + csBob.blockSize()+csBob.returnPCS(skBob)  < until) {
	    		System.arraycopy(t, now, transform, 0, csBob.blockSize()+csBob.returnPCS(skBob));
	    		byte[] get=csBob.decrypt(transform, skBob);
	    		System.arraycopy(get, 0, total, where1, get.length);
	    		now += csBob.blockSize()+csBob.returnPCS(skBob);
	    		where1 += csBob.blockSize();
	    	}
	    	else {
	    		System.arraycopy(t, now, transform, 0, until - now );
	    		byte[] get=csBob.decrypt(transform, skBob);
	    		System.arraycopy(get, 0, total, where1,  allocate - where1 );
	    		break;
	    	}
	    }	
		return total;
		
	}
    
    
    
    
    private void jsonArrayToList(JSONArray jsonarray, List<MessageRow> list) {
    	try{
    		for(int i=0;i<jsonarray.length();i++){
    			MessageRow row = MessageRow.parseMesssageRow(jsonarray.getJSONObject(i));
    			PTPLog.d(TAG, "jsonArrayToList: row : " + row.mMsg);
    			list.add(row);
    		}
    	}catch(JSONException e){
    		PTPLog.e(TAG, "jsonArrayToList: " + e.toString());
    	}
    }
    
    /**
     * chat message adapter from list adapter.
     * Responsible for how to show data to list fragment list view.
     */
    final class ChatMessageAdapter extends ArrayAdapter<MessageRow> {

    	public static final int VIEW_TYPE_MYMSG = 0;
    	public static final int VIEW_TYPE_INMSG = 1;
    	public static final int VIEW_TYPE_COUNT = 2;    // msg sent by me, or all incoming msgs
    	private LayoutInflater mInflater;
    	
		public ChatMessageAdapter(Context context, List<MessageRow> objects){
			super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
		
		@Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }
		
		@Override
        public int getItemViewType(int position) {
			MessageRow item = this.getItem(position);
			if ( item.mSender.equals(mApp.mDeviceName )){
				return VIEW_TYPE_MYMSG;
			}
			return VIEW_TYPE_INMSG;			
		}
		
		/**
		 * assemble each row view in the list view.
		 * http://dl.google.com/googleio/2010/android-world-of-listview-android.pdf
		 */
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;  // old view to re-use if possible. Useful for Heterogeneous list with diff item view type.
			MessageRow item = this.getItem(position);
			boolean mymsg = false;
			
			if ( getItemViewType(position) == VIEW_TYPE_MYMSG){
				if( view == null ){
	            	view = mInflater.inflate(R.layout.chat_row_mymsg, null);  // inflate chat row as list view row.
	            }
				mymsg = true;
				// view.setBackgroundResource(R.color.my_msg_background);
			} else {
				if( view == null ){
	            	view = mInflater.inflate(R.layout.chat_row_inmsg, null);  // inflate chat row as list view row.
	            }
				// view.setBackgroundResource(R.color.in_msg_background);
			}
			
            TextView sender = (TextView)view.findViewById(R.id.sender);
            sender.setText(item.mSender);
            
            TextView msgRow = (TextView)view.findViewById(R.id.msg_row);
            msgRow.setText(item.mMsg);
            if( mymsg ){
            	msgRow.setBackgroundResource(R.color.my_msg_background);	
            }else{
            	msgRow.setBackgroundResource(R.color.in_msg_background);
            }
            
            TextView time = (TextView)view.findViewById(R.id.time);
            time.setText(item.mTime);
            
            Log.d(TAG, "getView : " + item.mSender + " " + item.mMsg + " " + item.mTime);
            return view;
		}
    }
}

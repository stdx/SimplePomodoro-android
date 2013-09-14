package dacer.google.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;

import dacer.settinghelper.SettingUtility;

/**
 * Author:dacer
 * Date  :Sep 13, 2013
 * Used to deal with data between local and web
 */
public class TaskUtils {
	public static final String LIST_NAME = "SimplePomodoro";

	public static void initGuideList(Context c,Tasks tasks) throws IOException{
		TaskLocalUtils tLocalUtils;
		TaskWebUtils tWebUtils;
		tLocalUtils = new TaskLocalUtils(c);
		tWebUtils = new TaskWebUtils(tasks, c);
		if(tWebUtils.listExistOnWeb(LIST_NAME)){
			tLocalUtils.deleteList();
			String listId = tWebUtils.getListIdFromWeb(LIST_NAME);
			SettingUtility.setTaskListId(listId);
		}else{
			tLocalUtils.deleteList();
			tWebUtils.addTaskListToWeb(LIST_NAME);
			Task t = new Task();
			t.setTitle("Swipe to finish");
			String listId = tWebUtils.getListIdFromWeb(LIST_NAME);
			SettingUtility.setTaskListId(listId);
			tWebUtils.addTaskToWeb(listId, t);
		}
	  }
	public static void updateDB(String listId,Context c,Tasks tasks) throws IOException{
		//Read http://idacer.tk/?p=42
		  
		TaskLocalUtils tLocalUtils;
		TaskWebUtils tWebUtils;
		tLocalUtils = new TaskLocalUtils(c);
		tWebUtils = new TaskWebUtils(tasks, c);
		
		//FIRST Push and delete new tasks(identifrer == 0) in local and delete in DB
	    List<Task> localTaskList = tLocalUtils.getTasksFromDB();
	    for(Task t : localTaskList){
	    	if(t.getId().equals("0")){
	    		t.setId(null);
	    		tWebUtils.addTaskToWeb(listId, t);//may receive invalid listId
	    		t.setId("0");
	    		tLocalUtils.deleteTaskInDBTrue(t);
	    	}
	    }
	    
	    //SECOND
	    localTaskList = tLocalUtils.getTasksFromDB();
	    List<Task> webTaskList = tWebUtils.getTasksFromWeb(listId);//may receive invalid listId
	    List<Task> waitAddToLocalDB = new ArrayList<Task>();
	    for(Task t : webTaskList){
	    	boolean exist = false;
	    	for(Task tLocal : localTaskList){
	    		if(tLocal.getId().equals(t.getId())){
	    			exist = true;
	    		}
	    	}
	    	if(!exist){
	    		waitAddToLocalDB.add(t);
	    	}
	    }
	    tLocalUtils.putTasksToDB(waitAddToLocalDB);
	    
	    //THIRD
	    localTaskList = tLocalUtils.getTasksFromDB();
	    for(Task tLocal : localTaskList){
	    	for(Task tWeb : webTaskList){
	    		if(tLocal.getId().equals(tWeb.getId())){
	    			long tLocalUpdateTime = tLocal.getUpdated().getValue();
	    			long tWebUpdateTime = tWeb.getUpdated().getValue();
	    			if(tLocalUpdateTime > tWebUpdateTime){
	    				//update tLocal to web
	    				tWebUtils.updateTasktoWeb(listId, tLocal);
	    			}else if(tLocalUpdateTime < tWebUpdateTime){
	    				//update tWeb to local db
	    				tLocalUtils.updateTaskLocal(tWeb);
	    			}
	    		}
	    	}
	    }
	  }
}
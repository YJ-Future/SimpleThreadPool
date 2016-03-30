package com.gloryzyf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleThreadPool<Job extends Runnable> implements ThreadPool<Job>{
	
	//线程池最大容量
	private static final int MAX_WORKER_NUMBERS=10;
	//线程池默认容量
	private static final int DEFAULT_WORKER_NUMBERS=5;
	//线程池最小容量
	private static final int MIN_WORKER_NUMBERS=1;
	//Job队列
	private final LinkedList<Job> jobs=new LinkedList<Job>();
	//Worker队列
	private final List<Worker> workers=Collections.synchronizedList(new ArrayList<Worker>());
	//工作者线程的数量
	private int workerNum=DEFAULT_WORKER_NUMBERS;
	//线程编号生成
	private AtomicLong threadNum=new AtomicLong();
	
	public SimpleThreadPool(){
		initializeWorkers(DEFAULT_WORKER_NUMBERS);
	}
	public SimpleThreadPool(int num){
		initializeWorkers(num);
	}
	
	@Override
	public void execute(Job job) {
		// TODO Auto-generated method stub
		if(job!=null){
			//添加一个Job
			synchronized(jobs){
				jobs.addLast(job);
				jobs.notify();
			}
		}
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		for(Worker worker:workers){
			worker.shutdown();//
		}
	}
	
	@Override
	public void addWorkers(int num) {
		synchronized(jobs){
			//限制新增的Worker数量不能超过最大值
			if(num+workerNum>MAX_WORKER_NUMBERS){
				num=MAX_WORKER_NUMBERS-workerNum;
			}
			initializeWorkers(num);
			this.workerNum+=num;
		}
	}

	@Override
	public void removeWorkers(int num) {
		synchronized(jobs){
			if(num>=this.workerNum){
				throw new IllegalArgumentException("beyond workerNum");
			}
			int count=0;
			while(count<num){
				Worker worker=workers.get(count);
				if(workers.remove(worker)){
					worker.shutdown();
					count++;
				}
			}
			this.workerNum-=count;
		}
	}
	

	@Override
	public int getJobSize() {
		// TODO Auto-generated method stub
		return jobs.size();
	}
	//初始化线程工作者
	private void initializeWorkers(int num){
		for(int i=0;i<num;i++){
			Worker worker=new Worker();
			workers.add(worker);
			Thread thread=new Thread(worker,"ThreadPool-Worker-"+threadNum.incrementAndGet());
			thread.start();
		}
	}
	
	//Worker，负责消费Job
	class Worker implements Runnable{
		//是否工作
		private volatile boolean running=true;
		
		@Override
		public void run() {
			while(running){
				Job job=null;
				synchronized(jobs){
					//如果工作队列为空
					while(jobs.isEmpty()){
						try {
							jobs.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							//
						}
					}
					//取出一个job
					job=jobs.removeFirst();
				}
				if(job!=null){
					job.run();
					//
				}
			}
		}
		
		public void shutdown(){
			running=false;
		}
	}
}

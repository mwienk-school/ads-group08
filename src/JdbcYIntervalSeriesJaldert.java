import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.jfree.data.Range;
import org.jfree.data.time.Second;
import org.jfree.data.xy.YIntervalSeries;


public class JdbcYIntervalSeries extends YIntervalSeries {

	private Connection con;
	private String url;
	private String driverName;
	private String user;
	private String password;
	private String xAttribute;
	private String yAttribute;
	private String tableName;
	private String constraint;
	private double ds_start = 0;
	private double ds_extent = 0;

	protected int MAX_RESOLUTION = 100;
	
	private ArrayList<Integer> gcds;
	private HashMap<Long,Integer> counts;

	public JdbcYIntervalSeries(Comparable key) {
		super(key);
	}

	/**
	 * Creates a new dataset (initially empty) using the specified database connection.
	 * @param con
	 */
	public JdbcYIntervalSeries(Comparable key, Connection con){
		super(key);
		this.con=con;
	}

	/**
	 * Creates a new dataset using the specified database connection, 
	 * and populates it using data obtained with the supplied query. 
	 * @param con
	 * @param xAttribute
	 * @param yAttribute
	 * @param tableName
	 * @param constraint
	 */
	public JdbcYIntervalSeries(Comparable key, Connection con, 
			String xAttribute, String yAttribute, String tableName, String constraint){
		super(key);
		this.con = con;
		this.xAttribute=xAttribute;
		this.yAttribute = yAttribute;
		this.tableName = tableName;
		this.constraint = constraint;
	}


	/**
	 * Creates a new dataset (initially empty) and establishes a new database connection. 
	 * @param key
	 * @param url
	 * @param driverName
	 * @param user
	 * @param password
	 * @param xAttribute
	 * @param yAttribute
	 * @param tableName
	 * @param constraint
	 */
	public JdbcYIntervalSeries(Comparable key, String url, String driverName, String user, String password,
			String xAttribute, String yAttribute, String tableName, String constraint){
		super(key);
		this.url = url;
		this.driverName = driverName;
		this.user = user;
		this.password = password;
		getConnection();
		this.xAttribute=xAttribute;
		this.yAttribute = yAttribute;
		this.tableName = tableName;
		this.constraint = constraint;
	}

	/**
	 * return an existing connection. If the connection does not exists a new connection 
	 * is established.
	 * @return connection object
	 */
	protected Connection getConnection(){
		if(con==null)
			try {
				//Register the JDBC driver for MySQL.
				Class.forName(driverName);
				con = DriverManager.getConnection(url,user, password);
			} catch(Exception e){
				e.printStackTrace();
			}
			return con;
	}

	/**
	 * the range of the domain i.e. the x axis; 
	 * this is the overall range and not the range of the displayed data 
	 * @return range of the x axis
	 */
	public Range getDomainRange(){
		long maximumItemCount = 0;
		long minimumItemCount = 0;
		Connection con = getConnection();
		if(con==null) return null;
		Statement st;
		try {
			st = con.createStatement();
			String query = "select min(`"+xAttribute+"`) as MIN ,max(`"+xAttribute+"`) as MAX from "+tableName;
			if(constraint!=null && !constraint.isEmpty()) query+= " where "+constraint;
			ResultSet rs = st.executeQuery(query);
			rs.next();
			minimumItemCount = rs.getLong(1);
			maximumItemCount = rs.getLong(2);
			//			update(minimumItemCount,maximumItemCount);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new Range(minimumItemCount, maximumItemCount);
	}

	/**
	 * the range of the y axis; 
	 * this is the overall range and not the range of the displayed data 
	 * @return range of the y axis
	 */
	public Range getYRange(){
		double maximumItemCount = 0;
		double minimumItemCount = 0;
		Connection con = getConnection();
		if(con==null) return null;
		Statement st;
		try {
			st = con.createStatement();
			String query = "select min(`"+yAttribute+"`) as MIN ,max(`"+yAttribute+"`) from "+tableName;
			if(constraint!=null && !constraint.isEmpty()) query+= " where "+constraint;
			ResultSet rs = st.executeQuery(query);
			rs.next();
			maximumItemCount = rs.getDouble("MAX");
			minimumItemCount = rs.getDouble("MIN");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new Range(minimumItemCount, maximumItemCount);
	}

	/**
	 * specify the start and the extent of the data 
	 * @param start
	 * @param extent
	 */
	public void update(long start, long extent){
		long factor = (long) Math.ceil(extent/MAX_RESOLUTION);
		long ds_factor = (long) Math.ceil(ds_extent/MAX_RESOLUTION);
		if (start < ds_start || start > ds_start+ds_extent || 
				start+extent > ds_start+ds_extent ||
				factor < ds_factor/2 || factor > ds_factor*2 ){
			System.out.print("update with start, extent, factor, querytime: "+
					start+","+extent+","+factor);
			try {
				String insertQuery = "insert into factors (factor) values (?)";
				PreparedStatement insertStatement = con.prepareStatement(insertQuery);
				insertStatement.setLong(1, factor);
				insertStatement.executeUpdate();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			this.data.clear();			
			// load the data
			Connection con = getConnection();
			Object obj;
			if(con==null) return; 
			Statement st;
			try {
//				if (quantile==0){
					// this corresponds to min and max
					int ag_level = 1;
					if (extent > 200000000) {
						ag_level = 256;
					} else if (extent > 150000000) {
						ag_level = 128;
					} else if (extent > 100000000) {
						ag_level = 64;
					} else if (extent > 75000000) {
						ag_level = 32;
					} else if (extent > 50000000) {
						ag_level = 16;
					} else if (extent > 2500000) {
						ag_level = 8;
					} else if (extent > 1000000) {
						ag_level = 4;
					} else if (extent > 500000) {
						ag_level = 2;
					}
				
					String table = "dataset_";
					String query = "select "+xAttribute+", ID, avg("+yAttribute+"),min("+yAttribute+"),max("+yAttribute+") from "+table+ag_level+" where "+xAttribute+">="+(start-extent)+" and "+xAttribute+" <= "+(start+2*extent)+" group by "+xAttribute+" div "+factor;
					st = con.createStatement();
					long starttime = System.currentTimeMillis();
					ResultSet rs = st.executeQuery(query);
					System.out.println(","+(System.currentTimeMillis()-starttime));
					System.out.println("Selecting aggregation level: "+ag_level);
					long prevTime=0;
					while(rs.next()){
						long timed = rs.getLong(1);
						double pegelAvg = rs.getDouble(3);
						double pegelLow = rs.getDouble(4);
						double pegelHigh = rs.getDouble(5);
						if(prevTime!=timed){
							obj = new Second(new Date(timed));
							add(timed, pegelAvg, pegelLow, pegelHigh);
							prevTime= timed;
						} else 
							System.out.println("removed duplicate data at timestampt "+timed);
					}
//				} 
				} catch (SQLException e) {
				e.printStackTrace();
			}
			this.ds_start = start-extent;
			this.ds_extent = start+2*extent;
		}
		this.fireSeriesChanged();
	}

}
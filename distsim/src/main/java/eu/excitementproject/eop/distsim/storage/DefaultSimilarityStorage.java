/**
 * 
 */
package eu.excitementproject.eop.distsim.storage;


import java.io.FileNotFoundException;




import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.excitementproject.eop.common.utilities.configuration.ConfigurationException;
import eu.excitementproject.eop.common.utilities.configuration.ConfigurationParams;
import eu.excitementproject.eop.distsim.domains.FilterType;
import eu.excitementproject.eop.distsim.domains.RuleDirection;
import eu.excitementproject.eop.distsim.items.Element;
import eu.excitementproject.eop.distsim.scoring.ElementsSimilarityMeasure;
import eu.excitementproject.eop.distsim.scoring.similarity.ElementSimilarityScoring;
import eu.excitementproject.eop.distsim.storage.SimilarityStorage;
import eu.excitementproject.eop.distsim.util.Configuration;
import eu.excitementproject.eop.distsim.util.SortUtil;
import eu.excitementproject.eop.redis.RedisBasedStringListBasicMap;
import eu.excitementproject.eop.redis.RedisRunException;

/**
 * A general implementation of the {@link SimilarityStorage} interface, based on a given left and right similarity storages, and an element storage
 * 
 * @author Meni Adler
 * @since 09/09/2012
 *
 */
public class DefaultSimilarityStorage implements SimilarityStorage {
	
	private static final Logger logger = Logger.getLogger(DefaultSimilarityStorage.class);

	/**
	 * Constructs a SimilarityStorage, which is based on two Redis databases, one for left-to-right similarities and the second for right-to-left similarities.
	 *
	 * Assumption: The type of the elements is internally stored in the databases
	 * 
	 * @param leftElemntSimilarities A Redis database which maps LHS elements to their RHS similar elements, the RHS elements are assumed to be ordered descendingly by their similarity measures
	 * @param rightElemntSimilarities A Redis database which maps RHS elements to their LHS similar elements, the LHS elements are assumed to be ordered descendingly by their similarity measures
	 * @param resourceName The name of the resource (see eu.excitementproject.eop.common.component.Component#getComponentName()) 
	 * @param instanceName The name of the specific instance of this resource (@see eu.excitementproject.eop.common.component.Component#getInstanceName())
	 * @param elementClassName The name of type of the elements in the databases, e.g.: eu.excitementproject.eop.distsim.items.LemmaPosBasedElement, eu.excitementproject.eop.distsim.items.PredicateElement. 
	 * @throws ElementTypeException for any problem with the stored element type name in the databases 
	 */
	public DefaultSimilarityStorage(
			RedisBasedStringListBasicMap leftElemntSimilarities,
			RedisBasedStringListBasicMap rightElemntSimilarities,
			String resourceName,
			String instanceName) throws ElementTypeException {
		this.leftElemntSimilarities = leftElemntSimilarities;
		this.rightElemntSimilarities = rightElemntSimilarities;
		this.resourceName = resourceName;
		this.instanceName = instanceName;
		setElementClassName();
	}

	
	/**
	 * Constructs a SimilarityStorage, which is based on two Redis databases, one for left-to-right similarities and the second for right-to-left similarities.
	 *
	 * @param leftElemntSimilarities A Redis database which maps LHS elements to their RHS similar elements, the RHS elements are assumed to be ordered descendingly by their similarity measures
	 * @param rightElemntSimilarities A Redis database which maps RHS elements to their LHS similar elements, the LHS elements are assumed to be ordered descendingly by their similarity measures
	 * @param resourceName The name of the resource (see eu.excitementproject.eop.common.component.Component#getComponentName()) 
	 * @param instanceName The name of the specific instance of this resource (@see eu.excitementproject.eop.common.component.Component#getInstanceName())
	 * @param elementClassName The name of type of the elements in the databases, e.g.: eu.excitementproject.eop.distsim.items.LemmaPosBasedElement, eu.excitementproject.eop.distsim.items.PredicateElement. 
	 * @throws ElementTypeException 
	 */
	public DefaultSimilarityStorage(
			RedisBasedStringListBasicMap leftElemntSimilarities,
			RedisBasedStringListBasicMap rightElemntSimilarities,
			String resourceName,
			String instanceName,
			String elementClassName)  {
		this.leftElemntSimilarities = leftElemntSimilarities;
		this.rightElemntSimilarities = rightElemntSimilarities;
		this.resourceName = resourceName;
		this.instanceName = instanceName;
		this.elementClassName = elementClassName;
		
		
	}

	public DefaultSimilarityStorage(String l2rRedisFile, String r2lRedisFile, String resourceName, String instanceName, boolean bVM) throws ElementTypeException, RedisRunException, FileNotFoundException {
		//this.l2rRedisFile = l2rRedisFile;
		//this.r2lRedisFile= r2lRedisFile;
		this.leftElemntSimilarities = new RedisBasedStringListBasicMap(l2rRedisFile, bVM);
		if (r2lRedisFile != null) {
			this.rightElemntSimilarities = new RedisBasedStringListBasicMap(r2lRedisFile, bVM);
		} else
			this.rightElemntSimilarities = null;
		this.resourceName = resourceName;
		this.instanceName = resourceName;
		setElementClassName();
	}

	public DefaultSimilarityStorage(String leftRedisHost, int leftRedisPort, String resourceName, String instanceName) throws ElementTypeException, RedisRunException, FileNotFoundException {
		this(leftRedisHost, leftRedisPort,null, -1, resourceName, instanceName);
	}
	
	public DefaultSimilarityStorage(String leftRedisHost, int leftRedisPort, String rightRedisHost, int rightRedisPort, String resourceName, String instanceName) throws ElementTypeException {
		this.leftElemntSimilarities = new RedisBasedStringListBasicMap(leftRedisHost,leftRedisPort);
		if (rightRedisHost != null && rightRedisPort > 0) {
			this.rightElemntSimilarities = new RedisBasedStringListBasicMap(rightRedisHost,rightRedisPort);
		} else
			this.rightElemntSimilarities = null;
		this.resourceName = resourceName;
		this.instanceName = resourceName;
		setElementClassName();
	}
	
	/*public DefaultSimilarityStorage(ConfigurationParams params) throws ConfigurationException, ElementTypeException {
		this.leftElemntSimilarities = new RedisBasedStringListBasicMap(params.getString(Configuration.L2R_REDIS_HOST), params.getInt(Configuration.L2R_REDIS_PORT));
		try {
			this.rightElemntSimilarities = new RedisBasedStringListBasicMap(params.getString(Configuration.R2L_REDIS_HOST), params.getInt(Configuration.R2L_REDIS_PORT));
		} catch (ConfigurationException e) {
			this.rightElemntSimilarities = null;
		}
		this.resourceName = params.getString(Configuration.RESOURCE_NAME);
		try {
			this.instanceName = params.get(Configuration.INSTANCE_NAME);
		} catch (ConfigurationException e) {
			params.getConfigurationFile().toString();
		}
		try {
			setElementClassName();
		} catch (ElementTypeException e) {
			this.elementClassName = params.get(Configuration.ELEMENT_CLASS);
		}
	}*/

	/**
	 * Construct from configuration params.
	 * @param params contain at least the following: <ul>
	 * <li> l2r-redis-db-file
	 * <li> r2l-redis-db-file (optional)
	 * <li> resource-name
	 * <li> instance-name (optional)
	 * </ul>
	 * @throws ConfigurationException 
	 * @throws FileNotFoundException 
	 * @throws RedisRunException 
	 * @throws UnMatchElementTypeException for a case where the type of elements in one of the databases does not match the other
	 * 
	 */
	public DefaultSimilarityStorage(ConfigurationParams params) throws ConfigurationException, ElementTypeException, FileNotFoundException, RedisRunException {
		String redisDir = null;
		try {
			redisDir = params.get(eu.excitementproject.eop.redis.Configuration.REDIS_BIN_DIR);
		} catch (ConfigurationException e) {}
		
		boolean bVM = false;
		try {
			bVM = params.getBoolean(eu.excitementproject.eop.redis.Configuration.REDIS_VM);
		} catch (ConfigurationException e) {}
		
		String l2rRedisFile = params.get(Configuration.L2R_REDIS_DB_FILE);
		this.leftElemntSimilarities = (redisDir == null ? new RedisBasedStringListBasicMap(l2rRedisFile, bVM) : new RedisBasedStringListBasicMap(l2rRedisFile,redisDir, bVM));
		try {
			//this.
			String r2lRedisFile= params.get(Configuration.R2L_REDIS_DB_FILE);
			this.rightElemntSimilarities = (redisDir == null ? new RedisBasedStringListBasicMap(r2lRedisFile, bVM) : new RedisBasedStringListBasicMap(r2lRedisFile,redisDir, bVM));
		} catch (ConfigurationException e) {
			//this.l2rRedisFile = null;
			this.rightElemntSimilarities = null;
		}
		this.resourceName = params.get(Configuration.RESOURCE_NAME);
		try {
			this.instanceName = params.get(Configuration.INSTANCE_NAME);
		} catch (ConfigurationException e) {
			this.instanceName = params.getConfigurationFile().toString();
		}
		try {
			setElementClassName();
		} catch (ElementTypeException e) {
			this.elementClassName = params.get(Configuration.ELEMENT_CLASS);
		}

	}

	/**
	 * Resolves the class type of the elements in the left-to-right and right-to-left databases, as internally defined in each database
	 * 
	 * @throws Exception In case 
	 */
	protected void setElementClassName() throws ElementTypeException {
		String leftElementClassName = leftElemntSimilarities.getElementClassName();
		String rightElementClassName = (rightElemntSimilarities == null ? null : rightElemntSimilarities.getElementClassName());
		
		if (leftElementClassName == null) 
			throw new ElementTypeException("Left element type must be defined in the databases");
		
		if ((rightElementClassName != null) &&
			(!(leftElementClassName != null && leftElementClassName.equals(rightElementClassName)) || 
			 !(rightElementClassName != null && rightElementClassName.equals(leftElementClassName))))				
			throw new ElementTypeException("Different types of elements were found in the given left-2-right and right-2-left databases");
		this.elementClassName = leftElementClassName;		
	}
	
	/* (non-Javadoc)
	 * @see org.excitement.distsim.storage.SimilarityStorage#getSimilarityMeasure(org.excitement.distsim.items.Element, org.excitement.distsim.items.Element)
	 */
	@Override
	public List<ElementsSimilarityMeasure> getSimilarityMeasure(Element leftElement, Element rightElement) throws SimilarityNotFoundException {
		List<ElementsSimilarityMeasure> ret = new LinkedList<ElementsSimilarityMeasure>();
		try {
			Set<String> leftElementKeys = leftElement.toKeys();
			Set<String> rightElementKeys = rightElement.toKeys();
			
			for (String leftElementKey : leftElementKeys) {
				Element element1 = (Element) Class.forName(elementClassName).newInstance(); 
				element1.fromKey(leftElementKey);
				for (String rightElementKey : rightElementKeys) {
					Element element2 = (Element) Class.forName(elementClassName).newInstance(); 
					element2.fromKey(rightElementKey);
	
					String score = leftElemntSimilarities.get(leftElementKey,rightElementKey);
					if (score !=  null)
						ret.add(new DefaultElementsSimilarityMeasure(element1, element2,Double.parseDouble(score),null));
				}
			}
			
			if (leftElementKeys.size() > 1 && rightElementKeys.size() > 1)
				SortUtil.sortSimilarityRules(ret, true);
			return ret;
		} catch (Exception e) {
			throw new SimilarityNotFoundException(e);
		}
		
	}

	
	

	/* (non-Javadoc)
	 * @see org.excitement.distsim.storage.SimilarityStorage#getSimilarityMeasure(org.excitement.distsim.items.Element, org.excitement.distsim.domains.RuleDirection)
	 */
	@Override
	public List<ElementsSimilarityMeasure> getSimilarityMeasure(Element element,RuleDirection ruleDirection) throws SimilarityNotFoundException {
		return getSimilarityMeasure(element,ruleDirection,null, null,FilterType.ALL,0.0);
	}

	/* (non-Javadoc)
	 * @see org.excitement.distsim.storage.SimilarityStorage#getSimilarityMeasure(org.excitement.distsim.items.Element, org.excitement.distsim.domains.RuleDirection, org.excitement.distsim.storage.ElementFeatureScoreStorage)
	 */
	@Override
	public List<ElementsSimilarityMeasure> getSimilarityMeasure(Element element,
			RuleDirection ruleDirection,
			ElementFeatureScoreStorage elementFeatureScores,
			ElementSimilarityScoring elementSimilarityScoring) 
			throws SimilarityNotFoundException {
		return getSimilarityMeasure(element,ruleDirection,elementFeatureScores, elementSimilarityScoring,FilterType.ALL,0.0);
	}

	/* (non-Javadoc)
	 * @see org.excitement.distsim.storage.SimilarityStorage#getSimilarityMeasure(org.excitement.distsim.items.Element, org.excitement.distsim.domains.RuleDirection, org.excitement.distsim.domains.FilterType, double)
	 */
	@Override
	public List<ElementsSimilarityMeasure> getSimilarityMeasure(Element element,
			RuleDirection ruleDirection, FilterType filterType, double filterVal) throws SimilarityNotFoundException {
		return getSimilarityMeasure(element,ruleDirection,null, null,filterType,filterVal);
	}

	/* (non-Javadoc)
	 * @see org.excitement.distsim.storage.SimilarityStorage#getSimilarityMeasure(org.excitement.distsim.items.Element, org.excitement.distsim.domains.RuleDirection, org.excitement.distsim.storage.ElementFeatureScoreStorage, org.excitement.distsim.domains.FilterType, double)
	 */
	@Override
	public List<ElementsSimilarityMeasure> getSimilarityMeasure(Element element,
			RuleDirection ruleDirection,
			ElementFeatureScoreStorage elementFeatureScores,
			ElementSimilarityScoring elementSimilarityScoring,
			FilterType filterType, double filterVal) throws SimilarityNotFoundException {
		
		List<ElementsSimilarityMeasure> ret = new LinkedList<ElementsSimilarityMeasure>();	
		try {	
			List<ElementsSimilarityMeasure> tmp = new LinkedList<ElementsSimilarityMeasure>();
			       
			Set<String> element1Keys = element.toKeys();
			
			for (String element1Key : element1Keys) {
				
				Element element1 = (Element) Class.forName(elementClassName).newInstance(); 
				element1.fromKey(element1Key);
				
				List<String> elementSimilarities;
				if (ruleDirection == RuleDirection.LEFT_TO_RIGHT)
					elementSimilarities = leftElemntSimilarities.getTopN(element1Key,(filterType == FilterType.TOP_N ? (int)filterVal : Long.MAX_VALUE));
				else
					elementSimilarities = rightElemntSimilarities.getTopN(element1Key,(filterType == FilterType.TOP_N ? (int)filterVal : Long.MAX_VALUE));

				if (elementSimilarities == null) {
					logger.warn("No entry was found for key " + element1Key);
				} else {
					int i=0;
					for (String elementSimilarity : elementSimilarities) {
						
						//tmp
						//System.out.println(elementSimilarity);
						
						String[] toks = elementSimilarity.split(RedisBasedStringListBasicMap.ELEMENT_SCORE_DELIMITER);
						String element2Key = toks[0];
						Element element2 = (Element) Class.forName(elementClassName).newInstance(); 
						element2.fromKey(element2Key);
						double score = Double.parseDouble(toks[1]);

						tmp.add(ruleDirection == RuleDirection.LEFT_TO_RIGHT ? 
								new DefaultElementsSimilarityMeasure(element1, element2,score,null)
								: 
								new DefaultElementsSimilarityMeasure(element2, element1,score,null)
							);
						if (filterType != FilterType.TOP_N && filtered(filterType,filterVal,score,elementSimilarities.size(),i))
							break;
						i++;
					}							
				}					
			}
			
			if (element1Keys.size() > 1) {
				SortUtil.sortSimilarityRules(tmp, true); 			
				int i=1;
				for (ElementsSimilarityMeasure similarityRule :  tmp) {
					if (!filtered(filterType,filterVal,similarityRule.getSimilarityMeasure(),tmp.size(),i)) 
						ret.add(new DefaultElementsSimilarityMeasure(similarityRule.getLeftElement(),similarityRule.getRightElement(),similarityRule.getSimilarityMeasure(),similarityRule.getAdditionalInfo()));
					else
						break;
					i++;
				}
			} else
				ret = tmp;
			return ret;
		} catch (Exception e) {
			throw new SimilarityNotFoundException(e); 
		}			
	}



	protected boolean filtered(final FilterType type, final double filterVal, final double val, final int size, final int i) {
		switch (type) {
			case MIN_VAL:
				return val < filterVal;
			case TOP_N:
				return i > filterVal;
			case TOP_PRECENT:
				return i > size * filterVal;
			default:
				return false;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see eu.excitementproject.eop.common.component.Component#getComponentName()
	 */
	@Override
	public String getComponentName() {		
		return resourceName;
	}

	/* (non-Javadoc)
	 * @see eu.excitementproject.eop.common.component.Component#getInstanceName()
	 */
	@Override
	public String getInstanceName() {
		return instanceName;
	}
	
	/* (non-Javadoc)
	 * @see eu.excitementproject.eop.distsim.storage.SimilarityStorage#close()
	 */
	@Override
	public void close() {
		leftElemntSimilarities.close();
		if (rightElemntSimilarities != null)
			rightElemntSimilarities.close();
		
	}
	
	// the name of the l2r redis file (for closing)
	//protected String l2rRedisFile;
	// the name of the r2l redis file (for closing)
	//protected String r2lRedisFile;

	// Assumption: The similar elements each left-element are ordered descendingly by their similarity measures
	protected RedisBasedStringListBasicMap leftElemntSimilarities;	
	// Assumption: The similar elements of each right-element are ordered descendingly by their similarity measures	
	protected RedisBasedStringListBasicMap rightElemntSimilarities;
	protected String elementClassName;
	protected final String resourceName;
	protected String instanceName;


}

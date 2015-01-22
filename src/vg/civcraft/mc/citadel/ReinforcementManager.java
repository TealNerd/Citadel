package vg.civcraft.mc.citadel;

import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.block.Block;

import vg.civcraft.mc.citadel.database.CitadelReinforcementData;
import vg.civcraft.mc.citadel.misc.CitadelStatics;
import vg.civcraft.mc.citadel.misc.LoadingCacheNullException;
import vg.civcraft.mc.citadel.reinforcement.Reinforcement;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class ReinforcementManager {

	private CitadelReinforcementData db;

	// This shit is cool
	private RemovalListener<Location, Reinforcement> removalListener = new RemovalListener<Location, Reinforcement>() {
		public void onRemoval(
				RemovalNotification<Location, Reinforcement> removal) {
			Reinforcement rein = removal.getValue();
			saveReinforcement(rein);
		}
	};
	private LoadingCache<Location, Reinforcement> reinforcements = CacheBuilder
			.newBuilder().maximumSize(CitadelConfigManager.getMaxCacheSize())
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.removalListener(removalListener)
			.build(new CacheLoader<Location, Reinforcement>() {
				public Reinforcement load(Location loc) throws Exception {
					Reinforcement rein = db.getReinforcement(loc);
					if (rein == null) {
						throw new LoadingCacheNullException();
					}
					CitadelStatics.updateHitStat(CitadelStatics.LOAD);
					// decrement cache because it gets increased from getReinforcement()
					CitadelStatics.decrementHitStat(CitadelStatics.CACHE);
					return rein;
				}
			});

	public ReinforcementManager(CitadelReinforcementData db) {
		this.db = db;
	}

	/**
	 * Saves the reinforcement to the database. If the reinforcement durability
	 * is less than or equal to zero it will delete it from the database.
	 * @param The Reinforcement to save
	 */
	public void saveReinforcement(Reinforcement rein) {
		if (rein.getDurability() <= 0)
			deleteReinforcement(rein);
		CitadelStatics.updateHitStat(CitadelStatics.UPDATE);
		db.saveReinforcement(rein);
	}

	/**
	 * Saves the initial reinforcement into the database.
	 * @param The Reinforcement to save
	 */
	public void saveInitialReinforcement(Reinforcement rein) {
		reinforcements.put(rein.getLocation(), rein);
		CitadelStatics.updateHitStat(CitadelStatics.INSERT);
		db.insertReinforcement(rein);
	}

	/**
	 * Returns the Reinforcement from the specified location.
	 * 
	 * @param loc
	 * @return Reinforcement
	 */
	public Reinforcement getReinforcement(Location loc) {
		try {
			Reinforcement rein = reinforcements.get(loc);
			CitadelStatics.updateHitStat(CitadelStatics.CACHE);
			return rein;
		} catch (Exception e) {
			if (!(e.getCause() instanceof LoadingCacheNullException))
				e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the Reinforcement from the specified block.
	 * 
	 * @param block
	 * @return Reinforcement
	 */
	public Reinforcement getReinforcement(Block block) {
		return getReinforcement(block.getLocation());
	}

	/**
	 * Deletes the reinforcement. Should get called from the saveReinforcement
	 * method if the durability of the reinforcement is less than or equal to 0.
	 * 
	 * @param rein
	 */
	public void deleteReinforcement(Reinforcement rein) {
		reinforcements.invalidate(rein.getLocation());
		CitadelStatics.updateHitStat(CitadelStatics.DELETE);
		db.deleteReinforcement(rein);
	}

	/**
	 * Used to flush all the reinforcements to the db on shutdown. Can be called
	 * else where if too a manual flush is wanted.
	 */
	public void invalidateAllReinforcements() {
		reinforcements.invalidateAll();
	}

	/**
	 * @return Returns the next reinforcement Id for reinforcements.
	 */
	public int getNextReinforcementID() {
		return db.getLastReinId();
	}

	/**
	 * Returns if the location is reinforced or not.
	 * 
	 * @param loc
	 *            - The location of the potential reinforcement.
	 * @return Returns true if one was found.
	 */
	public boolean isReinforced(Location loc) {
		return getReinforcement(loc) != null;
	}

	/**
	 * Returns if the block is reinforced or not.
	 * 
	 * @param block
	 *            - The block of the potential reinforcement.
	 * @return Returns true if one was found.
	 */
	public boolean isReinforced(Block block) {
		return isReinforced(block.getLocation());
	}
}

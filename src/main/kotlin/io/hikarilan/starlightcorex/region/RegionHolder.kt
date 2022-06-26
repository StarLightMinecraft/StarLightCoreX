package io.hikarilan.starlightcorex.region

interface RegionHolder {

    fun addRegion(region: Region)

    fun hasRegion(region: Region): Boolean

    fun removeRegion(region: Region)

}

interface RegionHolderDefaultImpl : RegionHolder {

    val regions: MutableList<Region>

    override fun addRegion(region: Region) {
        regions.add(region)
    }

    override fun hasRegion(region: Region): Boolean {
        return regions.contains(region)
    }

    override fun removeRegion(region: Region) {
        regions.remove(region)
    }

}
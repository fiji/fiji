package mpicbg.spim.registration.bead;

import mpicbg.spim.registration.bead.descriptor.PointDescriptor;

public class OrderPointDescriptorMatch
{
	public OrderPointDescriptorMatch(PointDescriptor pd, double difference)
	{
		this.pd = pd;
		this.difference = difference;
	}
	public PointDescriptor pd;
	public double difference;
}

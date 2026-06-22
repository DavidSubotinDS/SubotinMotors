package lithan.abc.cars.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_store_order_item")
public class StoreOrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_order_item")
  private int idOrderItem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_order", nullable = false)
  private StoreOrder order;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_part", nullable = false)
  private CarPart part;

  @Column(nullable = false, length = 80)
  private String sku;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(name = "unit_price_minor", nullable = false)
  private long unitPriceMinor;

  @Column(nullable = false)
  private int quantity;

  public int getIdOrderItem() {
    return idOrderItem;
  }

  public StoreOrder getOrder() {
    return order;
  }

  public void setOrder(StoreOrder order) {
    this.order = order;
  }

  public CarPart getPart() {
    return part;
  }

  public void setPart(CarPart part) {
    this.part = part;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getPartName() {
    return partName;
  }

  public void setPartName(String partName) {
    this.partName = partName;
  }

  public long getUnitPriceMinor() {
    return unitPriceMinor;
  }

  public void setUnitPriceMinor(long unitPriceMinor) {
    this.unitPriceMinor = unitPriceMinor;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public long getLineTotalMinor() {
    return Math.multiplyExact(unitPriceMinor, quantity);
  }
}

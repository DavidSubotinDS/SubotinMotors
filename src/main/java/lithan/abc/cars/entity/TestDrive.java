package lithan.abc.cars.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "tb_test_drive")
public class TestDrive {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_test_drive")
  private int idTestDrive;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "test_drive_date", nullable = false)
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestDriveStatus status = TestDriveStatus.PENDING;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_car")
  private Car car;

  public TestDrive() {
  }

  public int getIdTestDrive() {
    return idTestDrive;
  }

  public void setIdTestDrive(int idTestDrive) {
    this.idTestDrive = idTestDrive;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public TestDriveStatus getStatus() {
    return status;
  }

  public void setStatus(TestDriveStatus status) {
    this.status = status;
  }

  public boolean isPending() {
    return status == TestDriveStatus.PENDING;
  }

  public boolean isAccepted() {
    return status == TestDriveStatus.ACCEPTED;
  }

  public boolean isRejected() {
    return status == TestDriveStatus.REJECTED;
  }

  public boolean isReschedulable() {
    return isPending() || isAccepted();
  }

  public boolean isCancellable() {
    return isReschedulable();
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
  }

  public Car getCar() {
    return car;
  }

  public void setCar(Car car) {
    this.car = car;
  }

}

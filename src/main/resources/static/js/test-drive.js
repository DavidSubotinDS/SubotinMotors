const dateNow = new Date();
const latestDate = new Date(dateNow);
latestDate.setDate(latestDate.getDate() + 30);

const toInputDate = (value) => {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const input = document.getElementById("inputDate");
if (input) {
  input.min = toInputDate(dateNow);
  input.max = toInputDate(latestDate);
}

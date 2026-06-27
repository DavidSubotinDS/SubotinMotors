import { useNavigate } from 'react-router-dom';

const INTERACTIVE_SELECTOR = 'a, button, input, select, textarea, label, [role="button"]';

export default function DataTable({ columns, rows, emptyText = 'No records found.', rowLink, rowLabel }) {
  const navigate = useNavigate();

  if (!rows?.length) {
    return <div className="empty-state">{emptyText}</div>;
  }

  function isInteractiveClick(target) {
    return target instanceof Element && Boolean(target.closest(INTERACTIVE_SELECTOR));
  }

  function openRow(path) {
    if (path) {
      navigate(path);
    }
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => {
            const path = rowLink?.(row) ?? null;
            const clickable = Boolean(path);
            return (
              <tr
                key={row.id ?? row.key ?? row.idTestDrive ?? row.idTestRide ?? index}
                className={clickable ? 'is-clickable' : undefined}
                role={clickable ? 'link' : undefined}
                tabIndex={clickable ? 0 : undefined}
                aria-label={clickable ? rowLabel?.(row) : undefined}
                onClick={(event) => {
                  if (!clickable || event.defaultPrevented || isInteractiveClick(event.target)) return;
                  openRow(path);
                }}
                onKeyDown={(event) => {
                  if (!clickable || isInteractiveClick(event.target)) return;
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openRow(path);
                  }
                }}
              >
                {columns.map((column) => (
                  <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

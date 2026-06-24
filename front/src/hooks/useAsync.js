import { useEffect, useState } from 'react';

export function useAsync(load, dependencies = []) {
  const [state, setState] = useState({
    data: null,
    error: null,
    loading: true,
  });

  useEffect(() => {
    let active = true;
    setState((current) => ({ ...current, loading: true, error: null }));

    load()
      .then((data) => {
        if (active) {
          setState({ data, error: null, loading: false });
        }
      })
      .catch((error) => {
        if (active) {
          setState({ data: null, error, loading: false });
        }
      });

    return () => {
      active = false;
    };
  }, dependencies);

  return state;
}
